package com.asalavei.cloudfilestorage.auth.user;

import com.asalavei.cloudfilestorage.auth.SignUpRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
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

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.debug("username '{}' is already taken", user.getUsername(), e);
            throw new UserAlreadyExistsException("Username is already taken.");
        }
    }

    public Optional<User> getUser(String username) {
        return userRepository.findByUsername(normalizeUsername(username));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}