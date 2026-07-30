package com.Lucca.Projeto1.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class JwtProperties {

    private static final int MIN_SECRET_BYTES = 32;
    private final String secret;
    private final long expirationSeconds;

    public JwtProperties(Environment environment) {
        this.secret = readRequired(environment, "JWT_SECRET");
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve possuir pelo menos 32 bytes"
            );
        }

        this.expirationSeconds = readExpiration(environment);
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private static String readRequired(Environment environment, String name) {
        String value = environment.getProperty(name);

        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "A variável de ambiente " + name + " deve ser configurada"
            );
        }

        return value;
    }

    private static long readExpiration(Environment environment) {
        String value = readRequired(environment, "JWT_EXPIRATION_SECONDS");

        try {
            long expiration = Long.parseLong(value);

            if (expiration <= 0) {
                throw new IllegalStateException(
                        "JWT_EXPIRATION_SECONDS deve ser maior que zero"
                );
            }

            return expiration;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "JWT_EXPIRATION_SECONDS deve ser um número inteiro"
            );
        }
    }
}
