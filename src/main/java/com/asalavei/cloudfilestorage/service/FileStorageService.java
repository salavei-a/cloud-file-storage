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

    public void upload(Long userId, MultipartFile file, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path + file.getOriginalFilename());
            minioRepository.putObject(bucketName, objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload: " + file.getOriginalFilename(), e);
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

    public InputStream download(Long userId, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path);
            return minioRepository.getObject(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download: " + path, e);
        }
    }

    public InputStream downloadAsZip(Long userId, String path) {
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
            throw new RuntimeException("Failed to download as zip: " + path, e);
        }
    }

    public List<ItemDto> list(Long userId, String path) {
        try {
            String basePrefix = String.format(BASE_PREFIX, userId);
            String prefix = String.format(OBJECT_NAME, userId, path);

            return minioRepository.findByPath(bucketName, basePrefix, prefix);
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
    public List<ItemDto> search(Long userId, String query) {
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

    public void rename(Long userId, String newName, String path) {
        try {
            String oldObjectName = String.format(OBJECT_NAME, userId, path);
            String newObjectName = String.format(OBJECT_NAME, userId, buildNewPath(path, newName));

            if (isFile(path)) {
                minioRepository.copyObject(bucketName, newObjectName, oldObjectName);
            } else {
                minioRepository.copyObjects(bucketName, newObjectName, oldObjectName);
            }

            delete(userId, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename: " + path, e);
        }
    }

    public void delete(Long userId, String path) {
        try {
            String objectName = String.format(OBJECT_NAME, userId, path);

            if (isFile(path)) {
                minioRepository.removeObject(bucketName, objectName);
            } else {
                minioRepository.removeObjects(bucketName, objectName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete: " + path, e);
        }
    }

    private String buildNewPath(String path, String newName) {
        String parentPath = getParentPath(path);
        return isFolder(path) ? parentPath + newName + "/" : parentPath + newName;
    }

    private String getParentPath(String path) {
        String trimmedPath = isFolder(path) ? path.substring(0, path.length() - 1) : path;
        return trimmedPath.substring(0, trimmedPath.lastIndexOf('/') + 1);
    }

    private boolean isFile(String path) {
        return !path.endsWith("/");
    }

    private boolean isFolder(String path) {
        return path.endsWith("/");
    }
}
