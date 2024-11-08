package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.REDIRECT_HOME;

@Controller
@RequiredArgsConstructor
@RequestMapping("/disk")
public class StorageController {

    private final StorageService storageService;

    @PostMapping
    public String upload(@RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        files.forEach(file -> storageService.uploadFile(userPrincipal.getId(), file));
        return REDIRECT_HOME;
    }

    @PostMapping("/{folderName}")
    public String createFolder(@PathVariable("folderName") String folderName, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        storageService.createFolder(userPrincipal.getId(), folderName);
        return REDIRECT_HOME;
    }
}
