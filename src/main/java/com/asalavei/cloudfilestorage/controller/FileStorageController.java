package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.FileStorageService;
import com.asalavei.cloudfilestorage.util.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.SEARCH_VIEW;

@Controller
@RequiredArgsConstructor
@RequestMapping("/storage")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @GetMapping("/download/{*path}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("path") String path,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InputStream inputStream = fileStorageService.downloadFile(userPrincipal.getId(), path);
        String fileName = fileStorageService.getFileName(path);
        String contentDisposition = "attachment; filename*=UTF-8''" + UriUtils.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Disposition", contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));

    }

    @GetMapping("/download-multiple/{*path}")
    public ResponseEntity<InputStreamResource> downloadFolder(@PathVariable("path") String path,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InputStream inputStream = fileStorageService.downloadFolderAsZip(userPrincipal.getId(), path);
        String fileName = fileStorageService.generateZipFilename(path);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/search")
    public String search(@RequestParam("query") String query, @AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        model.addAttribute("objects", fileStorageService.search(userPrincipal.getId(), query));
        return SEARCH_VIEW;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         @RequestParam(value = "path", defaultValue = "/") String path, RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        files.forEach(file -> fileStorageService.upload(userPrincipal.getId(), file, path));

        redirectAttributes.addFlashAttribute("message", "Upload complete");
        return HttpUtils.redirectToReferer(request);
    }

    @PostMapping("/{folderName}")
    public String createFolder(@PathVariable("folderName") String folderName, @AuthenticationPrincipal UserPrincipal userPrincipal,
                               @RequestParam(value = "path", defaultValue = "/") String path, RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {
        fileStorageService.createFolder(userPrincipal.getId(), folderName, path);

        redirectAttributes.addFlashAttribute("message", "Folder created successfully");
        return HttpUtils.redirectToReferer(request);
    }

    @PatchMapping
    public String rename(@RequestParam("newName") String newName, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         @RequestParam("path") String path, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.rename(userPrincipal.getId(), newName, path);

        redirectAttributes.addFlashAttribute("message", "Renamed successfully");
        return HttpUtils.redirectToReferer(request);
    }

    @DeleteMapping("/{*path}")
    public String delete(@PathVariable("path") String path, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.delete(userPrincipal.getId(), path);

        redirectAttributes.addFlashAttribute("message", "Deleted successfully");
        return HttpUtils.redirectToReferer(request);
    }
}
