package com.asalavei.cloudfilestorage.storage;

import com.asalavei.cloudfilestorage.validation.constraint.ValidObjectName;
import com.asalavei.cloudfilestorage.validation.constraint.ValidObjectPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class ObjectRequestDto {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @ValidObjectName
    String name;

    @NotBlank(message = "Path cannot be blank")
    @ValidObjectPath
    String path;
}
