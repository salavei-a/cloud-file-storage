package com.asalavei.cloudfilestorage.auth.user;

import com.asalavei.cloudfilestorage.auth.SignUpRequestDto;
import com.asalavei.cloudfilestorage.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.asalavei.cloudfilestorage.util.CredentialsUtil.normalizeUsername;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserService.class)
class UserServiceTest {

    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";
    private static final String NORMALIZED_USERNAME = "username";

    private static SignUpRequestDto signUpRequestDto;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16");

    @BeforeAll
    static void setUp() {
        signUpRequestDto = SignUpRequestDto.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .matchingPassword(PASSWORD)
                .build();
    }

    @Test
    void register_shouldSaveUser_whenUsernameIsAvailable() {
        userService.register(signUpRequestDto);

        User user = userRepository.findByUsername(NORMALIZED_USERNAME).get();
        assertEquals(NORMALIZED_USERNAME, user.getUsername());
    }

    @Test
    void register_shouldSaveUserWithNormalizedUsername_whenRequestUsernameIsUpperCase() {
        // Arrange
        String username = "UPPER_USERNAME";
        SignUpRequestDto upperCaseUsernameSignUpRequest = SignUpRequestDto.builder()
                .username(username)
                .password(USERNAME)
                .build();

        // Act
        userService.register(upperCaseUsernameSignUpRequest);

        // Assert
        String normalizedUsername = normalizeUsername(username);
        User user = userRepository.findByUsername(normalizedUsername).get();
        assertEquals(normalizedUsername, user.getUsername());
    }

    @Test
    void register_shouldThrowUserAlreadyExistsException_whenUsernameAlreadyExists() {
        userService.register(signUpRequestDto);

        Executable act = () -> userService.register(signUpRequestDto);

        assertThrows(UserAlreadyExistsException.class, act);
    }

    @Test
    void register_shouldHashPassword_whenUserRegisters() {
        userService.register(signUpRequestDto);

        User user = userRepository.findByUsername(NORMALIZED_USERNAME).get();
        assertNotEquals(PASSWORD, user.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPassword()));
    }

    @Test
    void getUser_shouldReturnUser_whenUsernameExists() {
        userService.register(signUpRequestDto);

        assertEquals(NORMALIZED_USERNAME, userService.getUser(USERNAME).get().getUsername());
    }

    @Test
    void getUser_shouldReturnEmptyOptional_whenUserDoesNotExist() {
        assertTrue(userService.getUser(USERNAME).isEmpty());
    }

    @TestConfiguration
    public static class Config {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}