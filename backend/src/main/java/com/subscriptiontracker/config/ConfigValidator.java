package com.subscriptiontracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfigValidator implements ApplicationRunner {

    private static final int TOTP_KEY_LENGTH = 32;

    private final JwtConfig jwtConfig;
    private final TotpConfig totpConfig;
    private final EmailConfig emailConfig;

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        if (isBlank(jwtConfig.getSecret())) {
            errors.add("JWT secret must be set (JWT_SECRET)");
        }
        if (isBlank(jwtConfig.getRefreshTokenPepper())) {
            errors.add("JWT refresh token pepper must be set (JWT_REFRESH_TOKEN_PEPPER)");
        }

        if (totpConfig.isEnabled()) {
            if (isBlank(totpConfig.getEncryptionKey())) {
                errors.add("TOTP encryption key must be set (TOTP_ENCRYPTION_KEY)");
            } else if (totpConfig.getEncryptionKey().length() != TOTP_KEY_LENGTH) {
                errors.add("TOTP encryption key must be exactly " + TOTP_KEY_LENGTH + " characters");
            }
        }

        if (emailConfig.isVerificationEnabled()) {
            if (isBlank(emailConfig.getResendApiKey())) {
                errors.add("RESEND_API_KEY must be set when email verification is enabled");
            }
            if (isBlank(emailConfig.getVerificationBaseUrl())) {
                errors.add("EMAIL_VERIFICATION_BASE_URL must be set when email verification is enabled");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid configuration: " + String.join("; ", errors));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
