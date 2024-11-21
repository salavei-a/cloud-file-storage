package com.asalavei.cloudfilestorage.repository;

import com.asalavei.cloudfilestorage.dto.MinioObjectDTO;
import com.asalavei.cloudfilestorage.exception.MinioOperationException;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
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
            log.error("Error saving object '{}' to bucket '{}'", path, bucketName, e);
            throw new MinioOperationException("Failed to save object: " + path);
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
        } catch (Exception e) {
            log.error("Error retrieving object '{}' from bucket '{}'", path, bucketName, e);
            throw new MinioOperationException("Failed to retrieve object: " + path);
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

            return inputStreams;
        } catch (Exception e) {
            log.error("Error retrieving objects from bucket '{}' with prefix '{}'", bucketName, prefix, e);
            throw new MinioOperationException("Failed to retrieve objects with prefix: " + prefix);
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
            log.error("Error copying object in bucket '{}' from '{}' to '{}'", bucketName, sourcePath, destinationPath, e);
            throw new MinioOperationException(String.format("Failed to copy object from: '%s' to: '%s'", sourcePath, destinationPath));
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
            log.error("Error copying objects in bucket '{}' from prefix '{}' to prefix '{}'", bucketName, sourcePrefix, destinationPrefix, e);
            throw new MinioOperationException(String.format("Failed to copy objects from prefix '%s' to prefix '%s'", sourcePrefix, destinationPrefix));
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
            log.error("Error deleting object '{}' from bucket '{}'", path, bucketName, e);
            throw new MinioOperationException("Failed to delete object: " + path);
        }
    }

    public void deleteAll(String bucketName, String prefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, true);
            List<DeleteObject> objectsToDelete = new ArrayList<>();

            for (Result<Item> result : results) {
                objectsToDelete.add(new DeleteObject(result.get().objectName()));
            }

            if (objectsToDelete.isEmpty()) {
                log.error("No objects found in bucket '{}' with prefix '{}'", bucketName, prefix);
                throw new MinioOperationException("No objects found to delete with prefix: " + prefix);
            }

            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build()
            );

            for (Result<DeleteError> error : errors) {
                DeleteError deleteError = error.get();
                log.error("Failed to delete object: {} - {}", deleteError.objectName(), deleteError.message());
            }
        } catch (Exception e) {
            log.error("Error deleting objects from bucket '{}' with prefix '{}'", bucketName, prefix, e);
            throw new MinioOperationException("Failed to delete objects with prefix: " + prefix);
        }
    }

    public List<MinioObjectDTO> list(String bucketName, String prefix, boolean recursive) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, recursive);
            List<MinioObjectDTO> minioObject = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                minioObject.add(
                        MinioObjectDTO.builder()
                                .path(objectName)
                                .build()
                );
            }

            return minioObject;
        } catch (Exception e) {
            log.error("Error listing objects from bucket '{}' with prefix '{}'", bucketName, prefix, e);
            throw new MinioOperationException(String.format("Failed to list objects with prefix '%s'", prefix));
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
