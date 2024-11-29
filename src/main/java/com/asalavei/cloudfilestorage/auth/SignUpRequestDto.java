package com.asalavei.cloudfilestorage.auth;

import com.asalavei.cloudfilestorage.validation.constraint.PasswordMatches;
import com.asalavei.cloudfilestorage.validation.constraint.ValidPassword;
import com.asalavei.cloudfilestorage.validation.constraint.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Builder
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
