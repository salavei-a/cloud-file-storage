package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final String BASE_PREFIX = "user-%s-files";
    private static final String OBJECT_NAME = BASE_PREFIX + "%s";

    @Value("${minio.bucket.name}")
    private String bucketName;

    private final MinioClient minioClient;

    @Override
    public void addFile(Long userId, MultipartFile file, String path) {
        String objectName = String.format(OBJECT_NAME, userId, path + file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) { // TODO: catch MinioException and custom exception StorageException
            throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public InputStream getFile(Long userId, String filePath) {
        String objectName = String.format(OBJECT_NAME, userId, filePath);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + filePath, e);
        }
    }

    @Override
    public InputStream getFolder(Long userId, String path) {
        try {
            String basePrefix = String.format(BASE_PREFIX, userId);
            String prefix = String.format(OBJECT_NAME, userId, path);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String trimmedPath = path.substring(0, path.length() - 1);
                String folderPrefix = trimmedPath.substring(0, trimmedPath.lastIndexOf('/') + 1);
                String objectName = item.objectName().replace(basePrefix + folderPrefix, "");

                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build()
                )) {
                    zipOutputStream.putNextEntry(new ZipEntry(objectName));
                    inputStream.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.finish();

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download folder: " + path, e);
        }
    }

    @Override
    public void createFolder(Long userId, String folderName, String path) {
        String objectName = String.format(OBJECT_NAME, userId, path + folderName + "/");

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .contentType("application/x-directory")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder: " + folderName, e);
        }
    }

    @Override
    public List<ItemDto> listItems(Long userId, String folderName) {
        String basePrefix = String.format(BASE_PREFIX, userId);
        String prefix = String.format(OBJECT_NAME, userId, folderName);
        List<ItemDto> items = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(false)
                            .build()
            );

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
        } catch (Exception e) {
            throw new RuntimeException("Failed to view files in folder: " + prefix, e);
        }

        return items;
    }

    @Override
    public List<ItemDto> searchItems(Long userId, String query) {
        try {
            String basePrefix = String.format(BASE_PREFIX, userId);
            Map<String, String> items = new HashMap<>();

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(basePrefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String originalPath = item.objectName().replace(basePrefix, "");
                String path = originalPath.endsWith("/")
                        ? originalPath
                        : originalPath.substring(0, originalPath.lastIndexOf("/") + 1);

                String[] parts = item.objectName().split("/");
                String objectName = parts[parts.length - 1];

                if (objectName.toLowerCase().contains(query.toLowerCase())) {
                    items.put(path, objectName);
                }

                Arrays.stream(parts)
                        .filter(part -> part.toLowerCase().contains(query.toLowerCase()))
                        .filter(part -> path.contains((part + "/")))
                        .forEach(part -> items.put(path.substring(0, path.indexOf(part) + part.length() + 1), part + "/"));
            }

            return items.entrySet().stream()
                    .map(entry -> new ItemDto(entry.getValue(), entry.getKey()))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search in storage", e);
        }
    }

    @Override
    public void delete(Long userId, String path) {
        if (!path.endsWith("/")) {
            deleteFile(userId, path);
        } else {
            deleteFolder(userId, path);
        }
    }

    private void deleteFile(Long userId, String filePath) {
        String objectName = String.format(OBJECT_NAME, userId, filePath);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    private void deleteFolder(Long userId, String folderPath) {
        String prefix = String.format(OBJECT_NAME, userId, folderPath);

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            List<DeleteObject> objectsToDelete = new ArrayList<>();

            for (Result<Item> result : results) {
                objectsToDelete.add(new DeleteObject(result.get().objectName()));
            }

            if (objectsToDelete.isEmpty()) {
                log.warn("No objects found with prefix: " + prefix);
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
                log.warn("Failed to delete: " + deleteError.objectName() + " - " + deleteError.message());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete folder: " + folderPath, e);
        }
    }
}
