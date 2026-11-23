package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import kr.co.daeun.flowmatebe.domain.github.repository.TeamGithubInstallationRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationMetadata;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamGithubInstallationRepository teamInstallationRepository =
            mock(TeamGithubInstallationRepository.class);
    private final GithubInstallationClient installationClient =
            mock(GithubInstallationClient.class);

    @Test
    void 로그인_사용자와_연결된_GitHub_App_설치_URL을_발급한다() {
        GithubAppProperties githubAppProperties =
                properties("flowmate-app");
        GithubInstallationStateService stateService =
                new GithubInstallationStateService(authProperties, githubAppProperties);
        GithubAppInstallationService service = new GithubAppInstallationService(
                githubAppProperties,
                stateService,
                teamRepository,
                teamInstallationRepository,
                installationClient
        );
        when(teamRepository.findByIdAndMemberUserId(10L, 42L))
                .thenReturn(Optional.of(team(10L)));

        var response = service.createInstallUrl(42L, 10L);

        assertThat(response.installUrl()).startsWith(
                "https://github.com/apps/flowmate-app/installations/new?state="
        );
        String state = response.installUrl().substring(response.installUrl().indexOf("state=") + 6);
        assertThat(state).contains(".");
        assertThat(stateService.validate(state, 42L).teamId()).isEqualTo(10L);
        assertThat(response.expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void GitHub_App_slug가_없으면_설치_URL을_발급하지_않는다() {
        GithubAppInstallationService service = new GithubAppInstallationService(
                properties(""),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createInstallUrl(42L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GitHub App이 설정되지 않았습니다.");
    }

    @Test
    void 설치_state는_다른_사용자가_사용할_수_없다() {
        GithubAppProperties properties =
                properties("flowmate-app");
        GithubInstallationStateService stateService =
                new GithubInstallationStateService(authProperties, properties);
        String state = stateService.create(42L, 10L).value();

        assertThatThrownBy(() -> stateService.validate(state, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("유효하지 않거나 만료되었습니다");
    }

    @Test
    void 설치를_완료하면_state의_팀에_Organization을_연결한다() {
        GithubAppProperties properties = properties("flowmate-app");
        GithubInstallationStateService stateService =
                new GithubInstallationStateService(authProperties, properties);
        GithubAppInstallationService service = new GithubAppInstallationService(
                properties,
                stateService,
                teamRepository,
                teamInstallationRepository,
                installationClient
        );
        Team team = team(10L);
        when(teamRepository.findByIdAndMemberUserId(10L, 42L))
                .thenReturn(Optional.of(team));
        when(teamInstallationRepository.findByTeam_Id(10L)).thenReturn(Optional.empty());
        when(installationClient.getInstallation(500L)).thenReturn(
                new GithubInstallationMetadata(
                        500L,
                        900L,
                        "Team-dAIving",
                        "Organization"
                )
        );
        String state = stateService.create(42L, 10L).value();

        service.completeInstallation(42L, 500L, state);

        verify(teamInstallationRepository).save(any());
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

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }
}
