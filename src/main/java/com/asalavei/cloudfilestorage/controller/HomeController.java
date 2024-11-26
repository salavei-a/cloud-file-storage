package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.FileStorageService;
import com.asalavei.cloudfilestorage.util.BreadcrumbsUtil;
import com.asalavei.cloudfilestorage.validation.ValidObjectPath;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.asalavei.cloudfilestorage.common.Constants.HOME_VIEW;

@Controller
@RequiredArgsConstructor
@Validated
@RequestMapping("/")
public class HomeController {

    private final FileStorageService fileStorageService;

    @GetMapping
    public String homePage(@RequestParam(value = "path", defaultValue = "/") @ValidObjectPath String path, Model model,
                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        model.addAttribute("breadcrumbs", BreadcrumbsUtil.buildBreadcrumbs(path));
        model.addAttribute("objects", fileStorageService.list(userPrincipal.getId(), path));
        return HOME_VIEW;
    }
}
