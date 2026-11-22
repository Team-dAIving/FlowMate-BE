package kr.co.daeun.flowmatebe.global.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String baseUrl
) {

    public AiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("AI_BASE_URL은 필수입니다.");
        }
    }
}
