package com.asalavei.cloudfilestorage.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final String OBJECT_NAME = "user-%s-files/%s";

    @Value("${minio.bucket.name}")
    private String bucketName;

    private final MinioClient minioClient;

    public void uploadFile(Long userId, MultipartFile file) {
        String objectName = String.format(OBJECT_NAME, userId, file.getOriginalFilename());

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

    public void createFolder(Long userId, String folderName) {
        String objectName = String.format(OBJECT_NAME, userId, folderName + "/");

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
}
