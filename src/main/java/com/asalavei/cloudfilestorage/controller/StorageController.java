package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.SEARCH_VIEW;

@Controller
@RequiredArgsConstructor
@RequestMapping("/storage")
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/download/{*filePath}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("filePath") String filePath,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            InputStream inputStream = storageService.getFile(userPrincipal.getId(), filePath);
            String filename = filePath.substring(filePath.lastIndexOf('/') + 1);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    @GetMapping("/download-multiple/{*path}")
    public ResponseEntity<InputStreamResource> downloadFolder(@PathVariable("path") String path,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            InputStream inputStream = storageService.getFolder(userPrincipal.getId(), path);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + generateZipFilename(path) + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public String searchItems(@RequestParam("query") String query, @AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        model.addAttribute("items", storageService.searchItems(userPrincipal.getId(), query));
        return SEARCH_VIEW;
    }

    @PostMapping
    public String upload(@RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         @RequestParam(value = "path", defaultValue = "/") String path) {
        files.forEach(file -> storageService.addFile(userPrincipal.getId(), file, path));
        return "redirect:/?path=" + path;
    }

    @PostMapping("/{folderName}")
    public String createFolder(@PathVariable("folderName") String folderName, @AuthenticationPrincipal UserPrincipal userPrincipal,
                               @RequestParam(value = "path", defaultValue = "/") String path) {
        storageService.createFolder(userPrincipal.getId(), folderName, path);
        return "redirect:/?path=" + path;
    }

    private static String generateZipFilename(String path) {
        String trimmedPath = path.substring(0, path.length() - 1);
        String folderName = trimmedPath.substring(trimmedPath.lastIndexOf("/") + 1);
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        return folderName + "-" + timestamp + ".zip";
    }
}
