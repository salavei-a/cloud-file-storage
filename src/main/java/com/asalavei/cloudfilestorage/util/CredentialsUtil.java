package com.asalavei.cloudfilestorage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CredentialsUtil {

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}
