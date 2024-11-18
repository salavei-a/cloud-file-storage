package com.asalavei.cloudfilestorage.dto;

import com.asalavei.cloudfilestorage.validation.PasswordMatches;
import com.asalavei.cloudfilestorage.validation.ValidPassword;
import com.asalavei.cloudfilestorage.validation.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
@PasswordMatches
public class SignUpRequestDto {

    @ValidUsername
    @NotBlank(message = "Username is required.")
    @Size(min = 1, max = 35, message = "Username must be between 1 and 35 characters long.")
    String username;

    @ValidPassword
    String password;

    @NotNull
    @Size(min = 1)
    String matchingPassword;
}
