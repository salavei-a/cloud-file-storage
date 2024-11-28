package com.asalavei.cloudfilestorage.config;

import com.asalavei.cloudfilestorage.security.CustomAuthenticationFailureHandler;
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

import static com.asalavei.cloudfilestorage.common.Constants.HOME_URL;
import static com.asalavei.cloudfilestorage.common.Constants.PROCESS_SIGNIN_URL;
import static com.asalavei.cloudfilestorage.common.Constants.SIGNIN_URL;
import static com.asalavei.cloudfilestorage.common.Constants.SIGNOUT_URL;

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
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl(SIGNIN_URL));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
