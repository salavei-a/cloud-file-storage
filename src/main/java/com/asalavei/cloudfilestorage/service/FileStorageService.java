package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import com.asalavei.cloudfilestorage.exception.FileStorageException;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${minio.bucket.name}")
    private String bucketName;

    private final MinioRepository minioRepository;

    public void upload(Long userId, MultipartFile file, String path) {
        try {
            String fullPath = getFullPath(userId, path + file.getOriginalFilename());
            minioRepository.save(bucketName, fullPath, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("Failed to upload file: {} for user: {} at path: {}", file.getOriginalFilename(), userId, path, e);
            throw new FileStorageException("Failed to upload file: " + file.getOriginalFilename());
        }
    }

    public void createFolder(Long userId, String folderName, String path) {
        String folderPath = getFullPath(userId, path + folderName.trim() + "/");
        minioRepository.save(bucketName, folderPath, new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
    }

    public InputStream download(Long userId, String path) {
        return minioRepository.get(bucketName, getFullPath(userId, path));
    }

    public InputStream downloadAsZip(Long userId, String path) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            String userRoot = getUserRoot(userId);
            String prefix = getFullPath(userId, path);

            Map<String, InputStream> objects = minioRepository.getAll(bucketName, prefix);

            for (Map.Entry<String, InputStream> entry : objects.entrySet()) {
                InputStream inputStream = entry.getValue();
                String fullObjectPath = entry.getKey();

                String trimmedPath = path.substring(0, path.length() - 1);
                String folderPrefix = trimmedPath.substring(0, trimmedPath.lastIndexOf('/') + 1);
                String relativePath = fullObjectPath.replace(userRoot + folderPrefix, "");

                zipOutputStream.putNextEntry(new ZipEntry(relativePath));
                inputStream.transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to create zip for user: {} at path: {}", userId, path, e);
            throw new FileStorageException("Failed to download folder as ZIP");
        }
    }

    public List<ItemDto> list(Long userId, String path) {
        String userRoot = getUserRoot(userId);
        String targetPath = getFullPath(userId, path);
        List<ItemDto> storedItems = minioRepository.list(bucketName, targetPath, false);
        List<ItemDto> items = new ArrayList<>();

        for (ItemDto item : storedItems) {
            String name = item.getPath().replace(targetPath, "");

            if (!name.isBlank()) {
                items.add(new ItemDto(name, item.getPath().replace(userRoot, "")));
            }
        }

        return items;
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
        String userRoot = getUserRoot(userId);
        String userRootPath = getFullPath(userId, "/");
        List<ItemDto> storedItems = minioRepository.list(bucketName, userRootPath, true);
        Set<ItemDto> items = new HashSet<>();

        for (ItemDto item : storedItems) {
            String path = item.getPath().replace(userRoot, "");

            if (isFile(item.getPath())) {
                items.add(new ItemDto(getFileName(path), getParentFolderPath(path)));
            }

            items.addAll(getParentFolders(path));
        }

        return items.stream()
                .filter(item -> item.getName().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    public void rename(Long userId, String newName, String path) {
        String oldPath = getFullPath(userId, path);
        String newPath = getFullPath(userId, buildNewPath(path, newName.trim()));

        if (isFile(path)) {
            minioRepository.copy(bucketName, newPath, oldPath);
        } else {
            minioRepository.copyAll(bucketName, newPath, oldPath);
        }

        delete(userId, path);
    }

    public void delete(Long userId, String path) {
        String fullPath = getFullPath(userId, path);

        if (isFile(path)) {
            minioRepository.delete(bucketName, fullPath);
        } else {
            minioRepository.deleteAll(bucketName, fullPath);
        }
    }

    private String getUserRoot(Long userId) {
        return String.format("user-%s-files", userId);
    }

    private String getFullPath(Long userId, String path) {
        return getUserRoot(userId) + path;
    }

    private String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private String getParentFolderPath(String path) {
        return path.substring(0, path.lastIndexOf("/") + 1);
    }

    private List<ItemDto> getParentFolders(String path) {
        return Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty() && path.contains(part + "/"))
                .map(part -> new ItemDto(
                        part + "/",
                        path.substring(0, path.indexOf(part) + part.length() + 1)
                ))
                .toList();
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
