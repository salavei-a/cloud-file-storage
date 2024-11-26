package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.dto.ObjectRequestDto;
import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.FileStorageService;
import com.asalavei.cloudfilestorage.util.HttpUtils;
import com.asalavei.cloudfilestorage.validation.ValidObjectPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.MESSAGE_ATTRIBUTE;
import static com.asalavei.cloudfilestorage.common.Constants.SEARCH_VIEW;

@Controller
@RequiredArgsConstructor
@Validated
@RequestMapping("/storage")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("path") @ValidObjectPath String path,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InputStream inputStream = fileStorageService.downloadFile(userPrincipal.getId(), path);
        String fileName = UriUtils.encode(fileStorageService.getFileName(path), StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename*=UTF-8''" + fileName;

        return ResponseEntity.ok()
                .header("Content-Disposition", contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/download-multiple")
    public ResponseEntity<InputStreamResource> downloadFolder(@RequestParam("path") @ValidObjectPath String path,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InputStream inputStream = fileStorageService.downloadFolderAsZip(userPrincipal.getId(), path);
        String fileName = UriUtils.encode(fileStorageService.generateZipFilename(path), StandardCharsets.UTF_8);

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
    public String upload(@RequestParam(value = "path", defaultValue = "/") @ValidObjectPath String path,
                         @RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        files.forEach(file -> fileStorageService.upload(userPrincipal.getId(), file, path));

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Upload complete");
        return HttpUtils.redirectToReferer(request);
    }

    @PostMapping("/folders")
    public String createFolder(@Valid ObjectRequestDto objectRequestDto, @AuthenticationPrincipal UserPrincipal userPrincipal,
                               RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.createFolder(userPrincipal.getId(), objectRequestDto.getName(), objectRequestDto.getPath());

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Folder created successfully");
        return HttpUtils.redirectToReferer(request);
    }

    @PatchMapping
    public String rename(@Valid ObjectRequestDto objectRequestDto, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.rename(userPrincipal.getId(), objectRequestDto.getName(), objectRequestDto.getPath());

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Renamed successfully");
        return HttpUtils.redirectToReferer(request);
    }

    @DeleteMapping
    public String delete(@RequestParam("path") @ValidObjectPath String path, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.delete(userPrincipal.getId(), path);

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Deleted successfully");
        return HttpUtils.redirectToReferer(request);
    }
}
