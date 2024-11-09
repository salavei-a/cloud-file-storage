package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final String PREFIX = "user-%s-files";
    private static final String OBJECT_NAME = PREFIX + "%s";

    @Value("${minio.bucket.name}")
    private String bucketName;

    private final MinioClient minioClient;

    @Override
    public void addFile(Long userId, MultipartFile file) {
        String objectName = String.format(OBJECT_NAME, userId, "/" + file.getOriginalFilename());

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
    public void createFolder(Long userId, String folderName) {
        String objectName = String.format(OBJECT_NAME, userId, "/" + folderName + "/");

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
        String prefix = String.format(PREFIX, userId);
        String folderPath = String.format(OBJECT_NAME, userId, folderName);
        List<ItemDto> items = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(folderPath)
                            .recursive(false)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                items.add(ItemDto.builder()
                        .name(item.objectName().replace(folderPath, ""))
                        .path(item.objectName().replace(prefix, ""))
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to view files in folder: " + folderPath, e);
        }

        return items;
    }
}
