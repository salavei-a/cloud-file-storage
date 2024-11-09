package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface StorageService {
    void addFile(Long userId, MultipartFile file);

    InputStream getFile(Long userId, String filename);

    void createFolder(Long userId, String folderName); // TODO: change folderName to folder to folderPath? to path?

    List<ItemDto> listItems(Long userId, String folderPath);
}
