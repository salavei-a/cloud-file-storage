package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.dto.ItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.asalavei.cloudfilestorage.common.Constants.SEARCH_VIEW;

@Controller
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    @GetMapping
    public String searchPage(Model model) {
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
}
