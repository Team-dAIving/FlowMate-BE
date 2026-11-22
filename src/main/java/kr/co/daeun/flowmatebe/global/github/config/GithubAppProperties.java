package kr.co.daeun.flowmatebe.global.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.github")
public record GithubAppProperties(
        String slug,
        Long appId,
        String privateKeyBase64,
        String webhookSecret,
        Duration installationStateTtl
) {

    private static final Duration DEFAULT_INSTALLATION_STATE_TTL = Duration.ofMinutes(10);

    public GithubAppProperties {
        installationStateTtl = installationStateTtl == null
                ? DEFAULT_INSTALLATION_STATE_TTL
                : installationStateTtl;
        if (installationStateTtl.isZero() || installationStateTtl.isNegative()) {
            throw new IllegalArgumentException("GitHub App 설치 state 유효 시간은 0보다 커야 합니다.");
        }
    }
}
