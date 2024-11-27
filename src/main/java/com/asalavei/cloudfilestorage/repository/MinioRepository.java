package com.asalavei.cloudfilestorage.repository;

import com.asalavei.cloudfilestorage.dto.MinioObjectDto;
import com.asalavei.cloudfilestorage.exception.MinioOperationException;
import com.asalavei.cloudfilestorage.exception.NoObjectFoundException;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    public void save(String bucketName, String path, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Failed to save object", e);
        }
    }

    public InputStream get(String bucketName, String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new NoObjectFoundException("No object found in MinIO");
            }
            throw new MinioOperationException(
                    String.format("Failed to retrieve object '%s'. Error code: %s, Message: %s",
                            path, e.errorResponse().code(), e.errorResponse().message()), e);
        } catch (Exception e) {
            throw new MinioOperationException("Failed to retrieve object");
        }
    }

    public Map<String, InputStream> getAll(String bucketName, String prefix) {
        try {
            Map<String, InputStream> inputStreams = new HashMap<>();
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, true);

            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                InputStream inputStream = get(bucketName, objectName);
                inputStreams.put(objectName, inputStream);
            }

            if (inputStreams.isEmpty()) {
                throw new NoObjectFoundException("No objects found in MinIO");
            }

            return inputStreams;
        } catch (NoObjectFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new MinioOperationException("Failed to retrieve objects", e);
        }
    }

    public List<MinioObjectDto> list(String bucketName, String prefix, boolean recursive) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, recursive);
            List<MinioObjectDto> minioObjects = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                minioObjects.add(new MinioObjectDto(item.objectName()));
            }

            return minioObjects;
        } catch (Exception e) {
            throw new MinioOperationException("Failed to list objects", e);
        }
    }

    public void copy(String bucketName, String destinationPath, String sourcePath) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(bucketName)
                    .object(sourcePath)
                    .build();

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(destinationPath)
                            .source(source)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Failed to copy object", e);
        }
    }

    public void copyAll(String bucketName, String destinationPrefix, String sourcePrefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, sourcePrefix, true);

            for (Result<Item> result : results) {
                String sourceObjectName = result.get().objectName();
                String relativePath = sourceObjectName.substring(sourcePrefix.length());
                String destinationObjectName = destinationPrefix + relativePath;

                copy(bucketName, destinationObjectName, sourceObjectName);
            }
        } catch (Exception e) {
            throw new MinioOperationException("Failed to copy objects", e);
        }
    }

    public void delete(String bucketName, String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioOperationException("Failed to delete object", e);
        }
    }

    public void deleteAll(String bucketName, String prefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, true);
            List<DeleteObject> objectsToDelete = new ArrayList<>();

            for (Result<Item> result : results) {
                objectsToDelete.add(new DeleteObject(result.get().objectName()));
            }

            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build()
            );

            List<String> errorMessages = new ArrayList<>();

            for (Result<DeleteError> error : errors) {
                DeleteError deleteError = error.get();
                errorMessages.add(String.format("Failed to delete object: '%s' - '%s'",
                        deleteError.objectName(), deleteError.message()));
            }

            if (!errorMessages.isEmpty()) {
                throw new MinioOperationException(
                        String.format("Errors occurred while deleting objects: %s", String.join(", ", errorMessages)));
            }
        } catch (NoObjectFoundException | MinioOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new MinioOperationException("Failed to delete objects", e);
        }
    }

    public boolean isObjectExists(String bucketName, String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );

            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new MinioOperationException(
                    String.format("Failed to check existence of object '%s'. Error code: %s, Message: %s",
                            path, e.errorResponse().code(), e.errorResponse().message()), e);
        } catch (Exception e) {
            throw new MinioOperationException("Failed to check existence of object", e);
        }
    }

    public boolean isFolderExists(String bucketName, String path) {
        try {
            if (!path.endsWith("/")) {
                path += "/";
            }

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(path)
                            .maxKeys(1)
                            .build()
            );

            return results.iterator().hasNext();
        } catch (Exception e) {
            throw new MinioOperationException("Failed to check existence of folder", e);
        }
    }

    private Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(recursive)
                        .build()
        );
    }
}
