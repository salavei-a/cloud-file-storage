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

    private final MinioRepository minioRepository;

    @Value("${minio.bucket.name}")
    private String bucketName;

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
                    String relativeObjectPath = fullObjectPath.replace(userRoot + getParentPath(path), "");

                    zipOutputStream.putNextEntry(new ZipEntry(relativeObjectPath));
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
        try {
            String userRoot = getUserRoot(userId);
            String targetPath = getFullPath(userId, path);
            String baseFolderName = getFolderName(path);

            List<MinioObjectDTO> minioObjects = minioRepository.list(bucketName, targetPath, false);
            List<MinioObjectDTO> userObjects = new ArrayList<>();

            for (MinioObjectDTO minioObject : minioObjects) {
                String objectName = getObjectName(minioObject.getPath());

                if (!objectName.equals(baseFolderName)) {
                    String objectPath = minioObject.getPath().replace(userRoot, "");

                    userObjects.add(MinioObjectDTO.builder()
                            .name(objectName)
                            .path(objectPath)
                            .isFolder(isFolder(objectPath))
                            .build()
                    );
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

                if (!isFolder(objectPath)) {
                    userObjects.add(MinioObjectDTO.builder()
                            .name(getFileName(objectPath))
                            .path(getParentFolderPath(objectPath))
                            .isFolder(false)
                            .build()
                    );
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

            if (isFolder(path)) {
                minioRepository.copyAll(bucketName, newPath, oldPath);
            } else {
                minioRepository.copy(bucketName, newPath, oldPath);
            }

            delete(userId, path);
        } catch (MinioOperationException e) {
            throw new FileStorageException("Unable to rename: " + getObjectName(path), e);
        }
    }

    public void delete(Long userId, String path) {
        try {
            String fullPath = getFullPath(userId, path);

            if (isFolder(path)) {
                minioRepository.deleteAll(bucketName, fullPath);
            } else {
                minioRepository.delete(bucketName, fullPath);
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
        if (isFolder(path)) {
            return getFolderName(path);
        } else {
            return getFileName(path);
        }
    }

    private String getUserRoot(Long userId) {
        return String.format("user-%s-files", userId);
    }

    private String normalizeObjectName(String objectName) {
        return objectName.replace("/", "");
    }

    private String getFullPath(Long userId, String path) {
        return getUserRoot(userId) + path;
    }

    private String getParentFolderPath(String path) {
        return path.substring(0, path.lastIndexOf("/") + 1);
    }

    private List<MinioObjectDTO> getParentFolders(String path) {
        List<MinioObjectDTO> parentFolders = new ArrayList<>();
        StringBuilder currentPath = new StringBuilder("/");

        for (String part : path.split("/")) {
            if (!part.isEmpty() && path.contains(part + "/")) {
                currentPath.append(part).append("/");
                parentFolders.add(MinioObjectDTO.builder()
                        .name(part)
                        .path(currentPath.toString())
                        .isFolder(true)
                        .build());
            }
        }

        return parentFolders;
    }

    private String buildNewPath(String path, String newName) {
        String parentPath = getParentPath(path);
        return isFolder(path) ? parentPath + newName + "/" : parentPath + newName;
    }

    private String getParentPath(String path) {
        String pathToExtractParent = isFolder(path) ? path.substring(0, path.length() - 1) : path;
        return pathToExtractParent.substring(0, pathToExtractParent.lastIndexOf('/') + 1);
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
