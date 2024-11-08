package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    void addFile(Long userId, MultipartFile file);

    void createFolder(Long userId, String folderName);

    List<ItemDto> listItems(Long userId, String folderPath);
}
