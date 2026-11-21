package kr.co.daeun.flowmatebe.global.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        String jwtIssuer,
        Duration accessTokenTtl,
        String frontendOrigin,
        String frontendRedirectUri
) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET은 32바이트 이상이어야 합니다.");
        }
    }
}
