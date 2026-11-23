package kr.co.daeun.flowmatebe.global.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String baseUrl,
        Duration transcriptionTimeout
) {

    public AiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("AI_BASE_URL은 필수입니다.");
        }
        if (transcriptionTimeout == null) {
            transcriptionTimeout = Duration.ofMinutes(10);
        }
        if (transcriptionTimeout.isNegative() || transcriptionTimeout.isZero()) {
            throw new IllegalArgumentException("AI_TRANSCRIPTION_TIMEOUT은 0보다 커야 합니다.");
        }
    }
}
