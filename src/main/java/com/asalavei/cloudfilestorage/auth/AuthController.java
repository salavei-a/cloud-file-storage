package com.asalavei.cloudfilestorage.auth;

import com.asalavei.cloudfilestorage.auth.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.asalavei.cloudfilestorage.util.Constants.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @GetMapping("/signin")
    public String signInForm(@ModelAttribute(USER_ATTRIBUTE) SignInRequestDto signInRequest) {
        if (isAuthenticated()) {
            return REDIRECT_HOME;
        }

        return SIGNIN_VIEW;
    }

    @PostMapping("/signin")
    public String signIn(@Valid @ModelAttribute(USER_ATTRIBUTE) SignInRequestDto signInRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return SIGNIN_VIEW;
        }

        return FORWARD_PROCESS_SIGNIN;
    }

    @GetMapping("/signup")
    public String signUpForm(@ModelAttribute(USER_ATTRIBUTE) SignUpRequestDto signUpRequest) {
        if (isAuthenticated()) {
            return REDIRECT_HOME;
        }

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

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals(ANONYMOUS_USER);
    }
}