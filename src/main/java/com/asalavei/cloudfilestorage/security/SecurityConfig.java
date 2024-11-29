package com.asalavei.cloudfilestorage.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.asalavei.cloudfilestorage.util.Constants.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HOME_URL, "/storage/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage(SIGNIN_URL)
                        .loginProcessingUrl(PROCESS_SIGNIN_URL)
                        .failureHandler(customAuthenticationFailureHandler)
                        .defaultSuccessUrl(HOME_URL, false))
                .logout(logout -> logout
                        .logoutUrl(SIGNOUT_URL)
                        .deleteCookies(SESSION_COOKIE_NAME)
                        .logoutSuccessUrl(SIGNIN_URL));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
