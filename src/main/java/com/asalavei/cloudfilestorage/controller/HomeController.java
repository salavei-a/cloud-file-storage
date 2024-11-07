package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.asalavei.cloudfilestorage.common.Constants.HOME_VIEW;
import static com.asalavei.cloudfilestorage.common.Constants.REDIRECT_HOME;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class HomeController {

    private final StorageService storageService;

    @GetMapping
    public String homePage() {
        return HOME_VIEW;
    }

    @PostMapping("/new-folder")
    public String createFolder(@RequestParam("folderName") String folderName, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        storageService.createFolder(userPrincipal.getId(), folderName);
        return REDIRECT_HOME;
    }
}
