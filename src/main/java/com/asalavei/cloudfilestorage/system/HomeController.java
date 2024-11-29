package com.asalavei.cloudfilestorage.system;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.storage.FileStorageService;
import com.asalavei.cloudfilestorage.util.BreadcrumbsUtil;
import com.asalavei.cloudfilestorage.validation.constraint.ValidObjectPath;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.asalavei.cloudfilestorage.common.Constants.*;
import static com.asalavei.cloudfilestorage.util.PathUtil.DELIMITER;

@Controller
@RequiredArgsConstructor
@Validated
@RequestMapping("/")
public class HomeController {

    private final FileStorageService fileStorageService;

    @GetMapping
    public String homePage(@RequestParam(value = PATH_PARAM, defaultValue = DELIMITER) @ValidObjectPath String path, Model model,
                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        model.addAttribute(BREADCRUMBS_ATTRIBUTE, BreadcrumbsUtil.buildBreadcrumbs(path));
        model.addAttribute(OBJECTS_ATTRIBUTE, fileStorageService.list(userPrincipal.getId(), path));
        return HOME_VIEW;
    }
}
