package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.MinioObjectDTO;
import com.asalavei.cloudfilestorage.exception.FileListingException;
import com.asalavei.cloudfilestorage.exception.FileStorageException;
import com.asalavei.cloudfilestorage.exception.MinioOperationException;
import com.asalavei.cloudfilestorage.repository.MinioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        } catch (IOException e) {
            log.error("IOException occurred while uploading file '{}' for user '{}' at path '{}'", file.getOriginalFilename(), userId, path);
            throw new FileStorageException("Unable to upload file: " + file.getOriginalFilename(), e);
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to upload file: " + file.getOriginalFilename(), e);
        }
    }

    public void createFolder(Long userId, String folderName, String path) {
        try {
            String folderPath = getFullPath(userId, path + normalizeObjectName(folderName) + "/");
            minioRepository.save(bucketName, folderPath, new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to create folder: " + folderName, e);
        }
    }

    public InputStream downloadFile(Long userId, String path) {
        try {
            return minioRepository.get(bucketName, getFullPath(userId, path));
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to download file: " + getFileName(path), e);
        }
    }

    public InputStream downloadFolderAsZip(Long userId, String path) {
        String userRoot = getUserRoot(userId);
        String prefix = getFullPath(userId, path);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            Map<String, InputStream> objects = minioRepository.getAll(bucketName, prefix);

            for (Map.Entry<String, InputStream> entry : objects.entrySet()) {
                try (InputStream inputStream = entry.getValue()) {
                    String fullObjectPath = entry.getKey();

                    String trimmedPath = path.substring(0, path.length() - 1);
                    String folderPrefix = trimmedPath.substring(0, trimmedPath.lastIndexOf('/') + 1);
                    String relativePath = fullObjectPath.replace(userRoot + folderPrefix, "");

                    zipOutputStream.putNextEntry(new ZipEntry(relativePath));
                    inputStream.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.finish();
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            log.error("IOException occurred while creating zip for user '{}' at path '{}'", userId, prefix, e);
            throw new FileStorageException("Unable to download folder: " + getFolderName(path), e);
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to download folder: " + getFolderName(path), e);
        }
    }

    public List<MinioObjectDTO> list(Long userId, String path) {
        String userRoot = getUserRoot(userId);
        String targetPath = getFullPath(userId, path);

        try {
            List<MinioObjectDTO> minioObjects = minioRepository.list(bucketName, targetPath, false);
            List<MinioObjectDTO> userObjects = new ArrayList<>();

            for (MinioObjectDTO minioObject : minioObjects) {
                String objectName = minioObject.getPath().replace(targetPath, "");

                if (!objectName.isBlank()) {
                    userObjects.add(new MinioObjectDTO(objectName, minioObject.getPath().replace(userRoot, "")));
                }
            }

            sortObjects(userObjects);

            return userObjects;
        } catch (MinioOperationException e) {
            throw new FileListingException("Unable to list files at path:" + path, e);
        }
    }

    /**
     * Searches for files and folders based on the specified query for a given user.
     * <ul>
     *   <li>If the object is a file, the returned path is the folder where the file is located.</li>
     *   <li>If the object is a folder, the returned path is the path of the folder itself.</li>
     * </ul>
     *
     * @param userId the ID of the user whose files and folders are being searched
     * @param query  the search query to filter objects by name (case-insensitive)
     * @return a list of {@link MinioObjectDTO} representing the found objects, each containing the object's name and path
     */
    public List<MinioObjectDTO> search(Long userId, String query) {
        String userRoot = getUserRoot(userId);
        String userRootPath = getFullPath(userId, "/");

        try {
            List<MinioObjectDTO> minioObjects = minioRepository.list(bucketName, userRootPath, true);
            Set<MinioObjectDTO> userObjects = new HashSet<>();

            for (MinioObjectDTO minioObject : minioObjects) {
                String objectPath = minioObject.getPath().replace(userRoot, "");

                if (isFile(objectPath)) {
                    userObjects.add(new MinioObjectDTO(getFileName(objectPath), getParentFolderPath(objectPath)));
                }

                userObjects.addAll(getParentFolders(objectPath));
            }

            return userObjects.stream()
                    .filter(object -> object.getName().toLowerCase().contains(query.toLowerCase()))
                    .sorted(Comparator.comparing(object -> object.getName().toLowerCase()))
                    .toList();
        } catch (MinioOperationException e) {
            log.error("Error searching objects for user '{}' with query '{}' at path '{}'", userId, query, userRootPath, e);
            throw new FileStorageException("Unable to search with query: " + query, e);
        }
    }

    public void rename(Long userId, String newName, String path) {
        try {
            String oldPath = getFullPath(userId, path);
            String newPath = getFullPath(userId, buildNewPath(path, normalizeObjectName(newName)));

            if (isFile(path)) {
                minioRepository.copy(bucketName, newPath, oldPath);
            } else {
                minioRepository.copyAll(bucketName, newPath, oldPath);
            }

            delete(userId, path);
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to rename: " + getObjectName(path), e);
        }
    }

    public void delete(Long userId, String path) {
        try {
            String fullPath = getFullPath(userId, path);

            if (isFile(path)) {
                minioRepository.delete(bucketName, fullPath);
            } else {
                minioRepository.deleteAll(bucketName, fullPath);
            }
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to delete: " + getObjectName(path), e);
        }
    }

    public String getFolderName(String path) {
        return getFileName(path.substring(0, path.length() - 1));
    }

    public String getFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String generateZipFilename(String path) {
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        return getFolderName(path) + "-" + timestamp + ".zip";
    }

    private String getObjectName(String path) {
        if (isFile(path)) {
            return getFileName(path);
        } else {
            return getFolderName(path);
        }
    }

    private String getUserRoot(Long userId) {
        return String.format("user-%s-files", userId);
    }

    private String normalizeObjectName(String objectName) {
        return objectName.trim();
    }

    private String getFullPath(Long userId, String path) {
        return getUserRoot(userId) + path;
    }

    private String getParentFolderPath(String path) {
        return path.substring(0, path.lastIndexOf("/") + 1);
    }

    private List<MinioObjectDTO> getParentFolders(String path) {
        return Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty() && path.contains(part + "/"))
                .map(part -> new MinioObjectDTO(
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

    private void sortObjects(List<MinioObjectDTO> userObjects) {
        userObjects.sort((o1, o2) -> {
            boolean isFolder1 = isFolder(o1.getPath());
            boolean isFolder2 = isFolder(o2.getPath());

            if (isFolder1 && !isFolder2) {
                return -1;
            }

            if (!isFolder1 && isFolder2) {
                return 1;
            }

            return o1.getPath().compareToIgnoreCase(o2.getPath());
        });
    }
}
