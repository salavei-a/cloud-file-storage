package com.asalavei.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.asalavei.cloudfilestorage.common.Constants.HOME_VIEW;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String homePage() {
        return HOME_VIEW;
    }
}
