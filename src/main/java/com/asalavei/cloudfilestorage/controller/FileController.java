package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.REDIRECT_HOME;

@Controller
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        storageService.uploadFile(userPrincipal.getId(), file);
        return REDIRECT_HOME;
    }

    @PostMapping("/upload-folder")
    public String uploadFolder(@RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        files.forEach(file -> storageService.uploadFile(userPrincipal.getId(), file));
        return REDIRECT_HOME;
    }
}
