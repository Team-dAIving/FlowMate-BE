package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubAppInstallationServiceTest {

    private final AuthProperties authProperties = new AuthProperties(
            "test-jwt-secret-must-be-at-least-32-bytes-long",
            "flowmate",
            Duration.ofHours(1),
            "http://localhost:5173",
            "http://localhost:5173/oauth/callback",
            "flowmate://oauth/callback",
            Duration.ofMinutes(2)
    );

    @Test
    void 로그인_사용자와_연결된_GitHub_App_설치_URL을_발급한다() {
        GithubAppProperties githubAppProperties =
                properties("flowmate-app");
        GithubInstallationStateService stateService =
                new GithubInstallationStateService(authProperties, githubAppProperties);
        GithubAppInstallationService service = new GithubAppInstallationService(
                githubAppProperties,
                stateService
        );

        var response = service.createInstallUrl(42L);

        assertThat(response.installUrl()).startsWith(
                "https://github.com/apps/flowmate-app/installations/new?state="
        );
        String state = response.installUrl().substring(response.installUrl().indexOf("state=") + 6);
        assertThat(state).contains(".");
        stateService.validate(state, 42L);
        assertThat(response.expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void GitHub_App_slug가_없으면_설치_URL을_발급하지_않는다() {
        GithubAppInstallationService service = new GithubAppInstallationService(
                properties(""),
                null
        );

        assertThatThrownBy(() -> service.createInstallUrl(42L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GitHub App이 설정되지 않았습니다.");
    }

    @Test
    void 설치_state는_다른_사용자가_사용할_수_없다() {
        GithubAppProperties properties =
                properties("flowmate-app");
        GithubInstallationStateService stateService =
                new GithubInstallationStateService(authProperties, properties);
        String state = stateService.create(42L).value();

        assertThatThrownBy(() -> stateService.validate(state, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("유효하지 않거나 만료되었습니다");
    }

    private GithubAppProperties properties(String slug) {
        return new GithubAppProperties(
                slug,
                null,
                null,
                null,
                Duration.ofMinutes(10)
        );
    }
}
