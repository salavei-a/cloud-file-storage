package com.asalavei.cloudfilestorage.service;

import com.asalavei.cloudfilestorage.dto.SignUpRequestDto;
import com.asalavei.cloudfilestorage.entity.User;
import com.asalavei.cloudfilestorage.exception.UserAlreadyExistsException;
import com.asalavei.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.asalavei.cloudfilestorage.util.CredentialsUtil.normalizeUsername;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> getUser(String username) {
        return userRepository.findByUsername(username);
    }

    public void register(SignUpRequestDto signUpRequest) {
        User user = User.builder()
                .username(normalizeUsername(signUpRequest.getUsername()))
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.debug("username={} is already taken", user.getUsername(), e);
            throw new UserAlreadyExistsException("Username is already taken.");
        }
    }
}