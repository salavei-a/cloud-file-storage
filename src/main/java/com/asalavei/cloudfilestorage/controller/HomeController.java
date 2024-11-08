package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.StorageService;
import com.asalavei.cloudfilestorage.util.BreadcrumbsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.asalavei.cloudfilestorage.common.Constants.HOME_VIEW;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class HomeController {

    private final StorageService storageService;

    @GetMapping
    public String homePage(@RequestParam(value = "path", required = false, defaultValue = "/") String path, Model model,
                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        model.addAttribute("breadcrumbs", BreadcrumbsUtil.buildBreadcrumbs(path));
        model.addAttribute("items", storageService.listItems(userPrincipal.getId(), path));
        return HOME_VIEW;
    }
}
