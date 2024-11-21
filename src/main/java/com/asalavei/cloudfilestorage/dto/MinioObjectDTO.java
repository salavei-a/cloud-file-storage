package com.asalavei.cloudfilestorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Builder
@Value
public class MinioObjectDTO {
    String name;
    String path;
}
