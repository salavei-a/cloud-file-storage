package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.REDIRECT_HOME;
import static com.asalavei.cloudfilestorage.common.Constants.SEARCH_VIEW;

@Controller
@RequiredArgsConstructor
@RequestMapping("/storage")
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/search")
    public String searchItems(Model model) {
        List<ItemDto> items = List.of(
                new ItemDto("3dd.png", "/3dd.png"),
                new ItemDto("New Text Document.txt", "/New Text Document.txt"),
                new ItemDto("Screenshot 2024-11-07 213438.png", "/test/Screenshot 2024-11-07 213438.png"),
                new ItemDto("ff/", "/test/ff/"),
                new ItemDto("1 (3).png", "/test/ff/efe/1 (3).png")
        );

        model.addAttribute("items", items);

        return SEARCH_VIEW;
    }

    @PostMapping
    public String upload(@RequestParam("files") List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        files.forEach(file -> storageService.addFile(userPrincipal.getId(), file));
        return REDIRECT_HOME;
    }

    @PostMapping("/{folderName}")
    public String createFolder(@PathVariable("folderName") String folderName, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        storageService.createFolder(userPrincipal.getId(), folderName);
        return REDIRECT_HOME;
    }
}
