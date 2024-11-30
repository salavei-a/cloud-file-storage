package com.asalavei.cloudfilestorage.storage;

import com.asalavei.cloudfilestorage.storage.exception.FileListingException;
import com.asalavei.cloudfilestorage.storage.exception.FileStorageException;
import com.asalavei.cloudfilestorage.storage.minio.MinioOperationException;
import com.asalavei.cloudfilestorage.storage.exception.ObjectNotFoundException;
import com.asalavei.cloudfilestorage.storage.exception.ObjectExistsException;
import com.asalavei.cloudfilestorage.storage.minio.MinioObjectDto;
import com.asalavei.cloudfilestorage.storage.minio.MinioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.asalavei.cloudfilestorage.util.PathUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioRepository minioRepository;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${storage.user-root-format}")
    private String userRootFormat;

    public void upload(Long userId, MultipartFile file, String path) {
        String fileName = file.getOriginalFilename();
        String fullPath = getFullPath(userId, path + fileName);

        try {
            if (isObjectExists(bucketName, fullPath)) {
                throw new ObjectExistsException("There is already a file or folder with file name you uploaded");
            }

            minioRepository.save(bucketName, fullPath, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (ObjectExistsException e) {
            log.info("File or folder already exists when uploading file '{}' for user '{}', bucket '{}', path '{}'",
                    file.getOriginalFilename(), userId, bucketName, fullPath);
            throw e;
        } catch (MinioOperationException | IOException e) {
            log.error("Error while uploading file '{}' for user '{}', bucket '{}', path '{}'",
                    file.getOriginalFilename(), userId, bucketName, fullPath, e);
            throw new FileStorageException("Unable to upload file: " + fileName);
        }
    }

    public void createFolder(Long userId, String folderName, String path) {
        String folderFullPath = getFullPath(userId, path + folderName + DELIMITER);

        try {
            if (isObjectExists(bucketName, folderFullPath)) {
                throw new ObjectExistsException("There is already a file or folder with folder name you created");
            }

            minioRepository.save(bucketName, folderFullPath, new ByteArrayInputStream(new byte[0]), 0, "application/x-directory");
        } catch (ObjectExistsException e) {
            log.info("File or folder already exists when creating folder '{}' for user '{}', bucket '{}', path '{}'",
                    folderName, userId, bucketName, folderFullPath);
            throw e;
        } catch (MinioOperationException e) {
            log.error("Error while creating folder '{}' for user '{}', bucket '{}', path '{}'",
                    folderName, userId, bucketName, folderFullPath, e);
            throw new FileStorageException("Unable to create folder: " + folderName);
        }
    }

    public InputStream downloadFile(Long userId, String path) {
        String fullPath = getFullPath(userId, path);

        try {
            return minioRepository.get(bucketName, fullPath);
        } catch (ObjectNotFoundException e) {
            log.warn("File not found to download '{}' for user '{}', bucket '{}'", fullPath, userId, bucketName, e);
            throw new FileStorageException(
                    String.format("Unable to download file '%s' because it does not exist", getFileName(path))
            );
        } catch (MinioOperationException e) {
            log.error("Error while downloading file for user '{}', bucket '{}', path '{}'",
                    userId, bucketName, fullPath, e);
            throw new FileStorageException("Unable to download file: " + getFileName(path));
        }
    }

    public InputStream downloadFolderAsZip(Long userId, String path) {
        String userRoot = getUserRoot(userId);
        String fullPath = getFullPath(userId, path);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            Map<String, InputStream> objects = minioRepository.getAll(bucketName, fullPath);

            for (Map.Entry<String, InputStream> entry : objects.entrySet()) {
                try (InputStream inputStream = entry.getValue()) {
                    String fullObjectPath = entry.getKey();
                    String relativeObjectPath = getRelativePath(fullObjectPath, userRoot + getParentPath(path));

                    zipOutputStream.putNextEntry(new ZipEntry(relativeObjectPath));
                    inputStream.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.finish();
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (ObjectNotFoundException e) {
            log.warn("Folder not found to download for user '{}', bucket '{}', path '{}'", userId, bucketName, fullPath, e);
            throw new FileStorageException(
                    String.format("Unable to download folder '%s' because it does not exist", getFolderName(path))
            );
        } catch (MinioOperationException | IOException e) {
            log.error("Error while downloading folder as zip for user '{}', bucket '{}', path '{}'",
                    userId, bucketName, fullPath, e);
            throw new FileStorageException("Unable to download folder: " + getFolderName(path));
        }
    }

    public List<ObjectResponseDto> list(Long userId, String path) {
        String userRoot = getUserRoot(userId);
        String fullPath = getFullPath(userId, path);

        try {
            if (!isFolder(path)) {
                log.warn("Failed to list objects for user '{}': provided path '{}' is not a folder", userId, path);
                throw new ObjectNotFoundException("Provided path is not a folder");
            }

            if (!isFolderExists(path, fullPath)) {
                log.warn("Failed to list objects for user '{}': folder does not exist bucket '{}', path '{}'", userId, bucketName, fullPath);
                throw new ObjectNotFoundException("Folder does not exist");
            }

            List<MinioObjectDto> minioObjects = minioRepository.list(bucketName, fullPath, false);
            List<ObjectResponseDto> userObjects = new ArrayList<>();

            for (MinioObjectDto minioObject : minioObjects) {
                String fullObjectPath = minioObject.name();
                String relativeObjectPath = getRelativePath(fullObjectPath, fullPath);

                if (!relativeObjectPath.isBlank()) {
                    String objectPath = getRelativePath(fullObjectPath, userRoot);

                    userObjects.add(ObjectResponseDto.builder()
                            .name(getObjectName(relativeObjectPath))
                            .path(objectPath)
                            .isFolder(isFolder(objectPath))
                            .build()
                    );
                }
            }

            sortObjects(userObjects);

            return userObjects;
        } catch (MinioOperationException e) {
            log.error("Error while listing objects for user '{}', bucket '{}', path '{}'", userId, bucketName, fullPath, e);
            throw new FileListingException("Unable to list files at path:" + path);
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
     * @return a list of {@link ObjectResponseDto} representing the found objects, each containing the object's name and path
     */
    public List<ObjectResponseDto> search(Long userId, String query) {
        String userRoot = getUserRoot(userId);
        String userRootPath = getFullPath(userId, DELIMITER);

        try {
            List<MinioObjectDto> minioObjects = minioRepository.list(bucketName, userRootPath, true);
            Set<ObjectResponseDto> userObjects = new HashSet<>();

            for (MinioObjectDto minioObject : minioObjects) {
                String objectPath = getRelativePath(minioObject.name(), userRoot);

                if (!isFolder(objectPath)) {
                    userObjects.add(ObjectResponseDto.builder()
                            .name(getFileName(objectPath))
                            .path(getParentFolderPath(objectPath))
                            .isFolder(false)
                            .build()
                    );
                }

                userObjects.addAll(getParentFolders(objectPath));
            }

            return userObjects.stream()
                    .filter(object -> object.getName().toLowerCase().contains(query.trim().toLowerCase()))
                    .sorted(Comparator.comparing(object -> object.getName().toLowerCase()))
                    .toList();
        } catch (MinioOperationException e) {
            log.error("Error while searching objects for user '{}' with query '{}', bucket '{}', path '{}'",
                    userId, query, bucketName, userRootPath, e);
            throw new FileStorageException("Unable to search with query: " + query);
        }
    }

    public void rename(Long userId, String newName, String path) {
        String sourcePath = getFullPath(userId, path);
        String destinationPath = getFullPath(userId, buildNewPath(path, newName));

        try {
            if (isObjectExists(bucketName, destinationPath)) {
                throw new ObjectExistsException("There is already a file or folder with name you specified. Specify a different name");
            }

            if (isFolder(path)) {
                minioRepository.copyAll(bucketName, destinationPath, sourcePath);
            } else {
                minioRepository.copy(bucketName, destinationPath, sourcePath);
            }

            delete(userId, path);
        } catch (ObjectExistsException e) {
            log.info("File or folder already exists when renaming from '{}' to '{}' for user '{}', bucket '{}'",
                    sourcePath, destinationPath, userId, bucketName);
            throw e;
        } catch (ObjectNotFoundException | FileStorageException e) {
            log.warn("No object found to rename for user '{}', bucket '{}', from '{}' to '{}'", userId, bucketName, sourcePath, destinationPath, e);
            throw new FileStorageException(String.format("Unable to rename '%s' because it does not exist", getObjectName(path)));
        } catch (MinioOperationException e) {
            log.error("Error while rename object for user '{}', bucket '{}', from '{}' to '{}'",
                    userId, bucketName, sourcePath, destinationPath, e);
            throw new FileStorageException("Unable to rename: " + getObjectName(path));
        }
    }

    public void delete(Long userId, String path) {
        String fullPath = getFullPath(userId, path);

        try {
            if (isFolder(path)) {
                minioRepository.deleteAll(bucketName, fullPath);
            } else {
                minioRepository.delete(bucketName, fullPath);
            }
        } catch (ObjectNotFoundException e) {
            log.warn("No objects found to delete for user '{}', bucket '{}', path '{}'", userId, bucketName, fullPath, e);
            throw new FileStorageException(String.format("Unable to delete '%s' because it does not exist", getObjectName(path)));
        } catch (MinioOperationException e) {
            log.error("Error while deleting object '{}' for user '{}' from bucket '{}'", fullPath, userId, bucketName, e);
            throw new FileStorageException("Unable to delete: " + getObjectName(path));
        }
    }

    private String getFullPath(Long userId, String path) {
        return getUserRoot(userId) + path;
    }

    private String getUserRoot(Long userId) {
        return String.format(userRootFormat, userId);
    }

    private boolean isObjectExists(String bucketName, String path) {
        if (minioRepository.isObjectExists(bucketName, path)) {
            return true;
        }

        if (isFolder(path)) {
            return minioRepository.isObjectExists(bucketName, path.substring(0, path.length() - 1));
        }

        return minioRepository.isObjectExists(bucketName, path + DELIMITER);
    }

    private boolean isFolderExists(String path, String fullPath) {
        if (DELIMITER.equals(path)) {
            return true;
        }

        return minioRepository.isObjectExists(bucketName, fullPath);
    }

    private void sortObjects(List<ObjectResponseDto> userObjects) {
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
