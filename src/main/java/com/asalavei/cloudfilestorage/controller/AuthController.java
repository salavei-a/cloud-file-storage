package com.asalavei.cloudfilestorage.controller;

import com.asalavei.cloudfilestorage.dto.SignInRequestDto;
import com.asalavei.cloudfilestorage.dto.SignUpRequestDto;
import com.asalavei.cloudfilestorage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.asalavei.cloudfilestorage.common.Constants.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @GetMapping("/signin")
    public String signInForm(@ModelAttribute(USER_ATTRIBUTE) SignInRequestDto signInRequest) {
        return SIGNIN_VIEW;
    }

    @PostMapping("/signin")
    public String signIn(@RequestParam(value = "error", required = false) String error,
                         @Valid @ModelAttribute(USER_ATTRIBUTE) SignInRequestDto signInRequest, BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            return SIGNIN_VIEW;
        }

        if ("true".equals(error)) {
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, "Incorrect username or password");
            return SIGNIN_VIEW;
        }

        return "forward:/auth/process-signin";
    }

    @GetMapping("/signup")
    public String signUpForm(@ModelAttribute(USER_ATTRIBUTE) SignUpRequestDto signUpRequest) {
        return SIGNUP_VIEW;
    }

    @PostMapping("/signup")
    public String signUp(@Valid @ModelAttribute(USER_ATTRIBUTE) SignUpRequestDto signUpRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return SIGNUP_VIEW;
        }

        userService.register(signUpRequest);
        return REDIRECT_SIGNIN;
    }
}