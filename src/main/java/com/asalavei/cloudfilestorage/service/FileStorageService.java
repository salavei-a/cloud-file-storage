package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import com.asalavei.cloudfilestorage.repository.MinioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final String BASE_PREFIX = "user-%s-files";
    private static final String OBJECT_NAME = BASE_PREFIX + "%s";

    @Value("${minio.bucket.name}")
    private String bucketName;

    private final MinioRepository minioRepository;

    public void uploadFile(Long userId, MultipartFile file, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path + file.getOriginalFilename());
            minioRepository.putObject(bucketName, objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
        }
    }

    public void createFolder(Long userId, String folderName, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path + folderName + "/");
            minioRepository.putObject(bucketName, objectName, new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder: " + folderName, e);
        }
    }

    public InputStream downloadFile(Long userId, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path);
            return minioRepository.getObject(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + path, e);
        }
    }

    public InputStream downloadFolder(Long userId, String path) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            String basePrefix = String.format(BASE_PREFIX, userId);
            String prefix = String.format(OBJECT_NAME, userId, path);
            Map<String, InputStream> objects = minioRepository.getObjects(bucketName, prefix);

            for (Map.Entry<String, InputStream> entry : objects.entrySet()) {
                InputStream inputStream = entry.getValue();
                String fullObjectName = entry.getKey();

                String trimmedPath = path.substring(0, path.length() - 1);
                String folderPrefix = trimmedPath.substring(0, trimmedPath.lastIndexOf('/') + 1);
                String objectName = fullObjectName.replace(basePrefix + folderPrefix, "");

                zipOutputStream.putNextEntry(new ZipEntry(objectName));
                inputStream.transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download folder: " + path, e);
        }
    }

    public List<ItemDto> listItems(Long userId, String path) {
        try {
            String basePrefix = String.format(BASE_PREFIX, userId);
            String prefix = String.format(OBJECT_NAME, userId, path);

            return minioRepository.listItems(bucketName, basePrefix, prefix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list items in folder: " + path, e);
        }
    }

    /**
     * Searches for files and folders based on the specified query for a given user.
     * <ul>
     *   <li>If the item is a file, the returned path is the folder where the file is located.</li>
     *   <li>If the item is a folder, the returned path is the path of the folder itself.</li>
     * </ul>
     *
     * @param userId the ID of the user whose files and folders are being searched
     * @param query  the search query to filter items by name (case-insensitive)
     * @return a list of {@link ItemDto} representing the found items, each containing the item's name and path
     */
    public List<ItemDto> searchItems(Long userId, String query) {
        try {
            String basePrefix = String.format(BASE_PREFIX, userId);
            List<ItemDto> items = minioRepository.findAll(bucketName, basePrefix);
            List<ItemDto> foundItems = new ArrayList<>();

            for (ItemDto item : items) {
                String name = item.getName();
                String path = item.getPath();

                if (isFile(path)) {
                    path = path.substring(0, path.lastIndexOf("/") + 1);
                }

                if (name.toLowerCase().contains(query.toLowerCase())) {
                    foundItems.add(new ItemDto(name, path));
                }
            }

            return foundItems;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by query: " + query, e);
        }
    }

    public void renameItem(Long userId, String newName, String path) {
        boolean isFolder = isFolder(path);
        String trimmedPath = isFolder ? path.substring(0, path.length() - 1) : path;
        String parentPath = trimmedPath.substring(0, trimmedPath.lastIndexOf('/') + 1);
        String newPath = parentPath + newName;
        newPath = isFolder ? newPath + "/" : newPath;

        if (isFolder) {
            renameFolder(userId, newPath, path);
        } else {
            renameFile(userId, newPath, path);
        }
    }

    private void renameFile(Long userId, String newPath, String oldPath) {
        try {
            String oldObjectName = String.format(OBJECT_NAME, userId, oldPath);
            String newObjectName = String.format(OBJECT_NAME, userId, newPath);

            minioRepository.copyObject(bucketName, oldObjectName, newObjectName);
            deleteFile(userId, oldPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename file: " + oldPath, e);
        }
    }

    private void renameFolder(Long userId, String newPath, String oldPath) {
        try {
            String oldPrefix = String.format(OBJECT_NAME, userId, oldPath);
            String newPrefix = String.format(OBJECT_NAME, userId, newPath);

            minioRepository.copyObjects(bucketName, newPrefix, oldPrefix);
            deleteFolder(userId, oldPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename folder: " + oldPath, e);
        }
    }

    public void deleteItem(Long userId, String path) {
        if (isFile(path)) {
            deleteFile(userId, path);
        } else {
            deleteFolder(userId, path);
        }
    }

    private void deleteFile(Long userId, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path);
            minioRepository.removeObject(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + path, e);
        }
    }

    private void deleteFolder(Long userId, String path) {
        try {
            String prefix = String.format(OBJECT_NAME, userId, path);
            minioRepository.removeObjects(bucketName, prefix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete folder: " + path, e);
        }
    }

    private boolean isFile(String path) {
        return !path.endsWith("/");
    }

    private boolean isFolder(String path) {
        return path.endsWith("/");
    }
}
