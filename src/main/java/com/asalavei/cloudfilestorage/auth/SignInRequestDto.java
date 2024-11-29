package com.asalavei.cloudfilestorage.auth;

import com.asalavei.cloudfilestorage.validation.constraint.ValidPassword;
import com.asalavei.cloudfilestorage.validation.constraint.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class SignInRequestDto {

    @ValidUsername
    @NotBlank(message = "Username is required.")
    @Size(min = 1, max = 35, message = "Username must be between 1 and 35 characters long.")
    String username;

    @ValidPassword
    String password;
}
