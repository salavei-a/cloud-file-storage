package com.asalavei.cloudfilestorage.repository;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    public void putObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (MinioException e) {
            log.error("MinIO error while putting object '{}' in bucket '{}': {}", objectName, bucketName, e.getMessage());
            throw new RuntimeException("Failed to put object in storage: " + objectName, e);
        } catch (Exception e) {
            log.error("Unexpected error while putting object '{}' in bucket '{}': {}", objectName, bucketName, e.getMessage());
            throw new RuntimeException("Unexpected error during put operation for object: " + objectName, e);
        }
    }

    public InputStream getObject(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object: " + objectName, e);
        }
    }

    public Map<String, InputStream> getObjects(String bucketName, String prefix) {
        try {
            Map<String, InputStream> inputStreams = new HashMap<>();
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, true);

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                InputStream inputStream = getObject(bucketName, item.objectName());
                inputStreams.put(objectName, inputStream);
            }

            return inputStreams;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get objects: " + prefix, e);
        }
    }

    public void copyObject(String bucketName, String newObjectName, String oldObjectName) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(bucketName)
                    .object(oldObjectName)
                    .build();

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectName)
                            .source(source)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object: " + oldObjectName, e);
        }
    }

    public void copyObjects(String bucketName, String newPrefix, String oldPrefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, oldPrefix, true);

            for (Result<Item> result : results) {
                Item item = result.get();
                String oldObjectName = item.objectName();
                String relativePath = oldObjectName.substring(oldPrefix.length());
                String newObjectName = newPrefix + relativePath;

                copyObject(bucketName, newObjectName, oldObjectName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy objects: ", e);
        }
    }

    public List<ItemDto> findByPath(String bucketName, String basePrefix, String prefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, false);
            List<ItemDto> items = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName().replace(prefix, "");

                if (!objectName.isBlank()) {
                    items.add(ItemDto.builder()
                            .name(objectName)
                            .path(item.objectName().replace(basePrefix, ""))
                            .build());
                }
            }

            return items;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find objects in: " + prefix, e);
        }
    }

    public List<ItemDto> findAll(String bucketName, String basePrefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, basePrefix, true);
            Map<String, String> items = new HashMap<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String path = item.objectName().replace(basePrefix, "");
                String[] parts = item.objectName().split("/");
                String objectName = parts[parts.length - 1];

                items.put(path, objectName);

                Arrays.stream(parts)
                        .filter(part -> path.contains((part + "/")))
                        .forEach(part -> items.put(path.substring(0, path.indexOf(part) + part.length() + 1), part + "/"));
            }

            return items.entrySet().stream()
                    .map(entry -> ItemDto.builder()
                            .name(entry.getValue())
                            .path(entry.getKey())
                            .build())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all objects in: " + bucketName + basePrefix, e);
        }
    }

    public void removeObject(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove object: " + objectName, e);
        }
    }

    public void removeObjects(String bucketName, String prefix) {
        try {
            Iterable<Result<Item>> results = listObjects(bucketName, prefix, true);
            List<DeleteObject> objectsToDelete = new ArrayList<>();

            for (Result<Item> result : results) {
                objectsToDelete.add(new DeleteObject(result.get().objectName()));
            }

            if (objectsToDelete.isEmpty()) {
                log.error("No objects found with prefix: " + prefix);
                throw new RuntimeException("No object found to delete");
            }

            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build()
            );

            for (Result<DeleteError> error : errors) {
                DeleteError deleteError = error.get();
                log.error("Failed to remove: " + deleteError.objectName() + " - " + deleteError.message());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove objects", e);
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
