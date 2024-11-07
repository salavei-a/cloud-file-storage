package com.asalavei.cloudfilestorage.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    void uploadFile(Long userId, MultipartFile file);

    void createFolder(Long userId, String folderName);
}
