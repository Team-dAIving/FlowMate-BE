package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.entity.TeamGithubInstallation;
import kr.co.daeun.flowmatebe.domain.github.repository.TeamGithubInstallationRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationRepositories;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubInstallationQueryServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamGithubInstallationRepository teamInstallationRepository =
            mock(TeamGithubInstallationRepository.class);
    private final GithubInstallationClient installationClient =
            mock(GithubInstallationClient.class);
    private final GithubInstallationQueryService service = new GithubInstallationQueryService(
            teamRepository,
            teamInstallationRepository,
            installationClient
    );

    @Test
    void 팀에_연결된_Organization과_저장소만_조회한다() {
        Team team = team(10L);
        TeamGithubInstallation installation = installation(team);
        when(teamRepository.findByIdAndMemberUserId(10L, 1L))
                .thenReturn(Optional.of(team));
        when(teamInstallationRepository.findByTeam_Id(10L))
                .thenReturn(Optional.of(installation));
        when(installationClient.listRepositories(500L)).thenReturn(
                new GithubInstallationRepositories(500L, List.of(
                        new GithubInstallationRepositories.Repository(
                                1001L,
                                "Team-dAIving",
                                "FlowMate-BE",
                                "Team-dAIving/FlowMate-BE",
                                "https://github.com/Team-dAIving/FlowMate-BE",
                                true
                        )
                ))
        );

        var response = service.getTeamInstallation(1L, 10L);

        assertThat(response.installations()).singleElement().satisfies(item -> {
            assertThat(item.installationId()).isEqualTo(500L);
            assertThat(item.accountLogin()).isEqualTo("Team-dAIving");
            assertThat(item.repositories()).hasSize(1);
        });
    }

    @Test
    void 팀에_연결이_없으면_빈_목록을_반환한다() {
        when(teamRepository.findByIdAndMemberUserId(10L, 1L))
                .thenReturn(Optional.of(team(10L)));
        when(teamInstallationRepository.findByTeam_Id(10L)).thenReturn(Optional.empty());

        var response = service.getTeamInstallation(1L, 10L);

        assertThat(response.installations()).isEmpty();
        verify(installationClient, never()).listRepositories(500L);
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }

    private TeamGithubInstallation installation(Team team) {
        TeamGithubInstallation installation = new TeamGithubInstallation();
        installation.setTeam(team);
        installation.setInstallationId(500L);
        installation.setGithubAccountId(900L);
        installation.setAccountLogin("Team-dAIving");
        return installation;
    }
}
