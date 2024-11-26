package com.asalavei.cloudfilestorage.dto;

import com.asalavei.cloudfilestorage.validation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class ObjectRequestDto {

    @NotBlank(message = "Name can't be blank")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Pattern(regexp = "^[^/]*$", message = "Name can't contain the '/' character")
    String name;

    @NotBlank(message = "Path can't be blank")
    @ValidPath
    String path;
}
