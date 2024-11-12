package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface StorageService {
    void addFile(Long userId, MultipartFile file, String path);

    InputStream getFile(Long userId, String filePath);

    void createFolder(Long userId, String folderName, String path);

    List<ItemDto> listItems(Long userId, String folderPath);

    List<ItemDto> searchItems(Long userId, String query);
}
