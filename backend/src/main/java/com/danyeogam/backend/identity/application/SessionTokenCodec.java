package com.danyeogam.backend.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SessionTokenCodec {

    private static final int TOKEN_BYTES = 32;
    private static final int ENCODED_LENGTH = 43;
    private static final Pattern URL_SAFE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean isValidFormat(String rawToken) {
        if (rawToken == null || rawToken.length() != ENCODED_LENGTH
                || !URL_SAFE_TOKEN.matcher(rawToken).matches()) {
            return false;
        }
        try {
            return Base64.getUrlDecoder().decode(rawToken).length == TOKEN_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public byte[] hash(String rawToken) {
        if (!isValidFormat(rawToken)) {
            throw new IllegalArgumentException("세션 토큰 형식이 올바르지 않습니다.");
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
