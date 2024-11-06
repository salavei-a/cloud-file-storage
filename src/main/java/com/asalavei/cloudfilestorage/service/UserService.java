package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.SignUpRequestDto;
import com.asalavei.cloudfilestorage.entity.User;
import com.asalavei.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.asalavei.cloudfilestorage.util.CredentialsUtil.normalizeUsername;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(SignUpRequestDto signUpRequest) {
        User user = User.builder()
                .username(normalizeUsername(signUpRequest.getUsername()))
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .build();
        userRepository.save(user);
    }
}