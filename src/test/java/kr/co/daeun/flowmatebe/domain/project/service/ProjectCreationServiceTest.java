package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.request.CreateProjectRequest;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepositoryLinkRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamMemberRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.global.github.dto.GithubIssueMetadata;
import kr.co.daeun.flowmatebe.global.github.dto.GithubRepositoryMetadata;
import kr.co.daeun.flowmatebe.global.github.dto.GithubRepositorySnapshot;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCreationServiceTest {

    private final kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository projectRepository =
            mock(kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final ProjectRepositoryLinkRepository repositoryLinkRepository =
            mock(ProjectRepositoryLinkRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
    private final GithubInstallationClient githubClient = mock(GithubInstallationClient.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ProjectCreationService service = new ProjectCreationService(
            projectRepository,
            projectMemberRepository,
            repositoryLinkRepository,
            teamRepository,
            teamMemberRepository,
            taskRepository,
            githubClient
    );

    @Test
    void 실제_GitHub_저장소를_검증하고_프로젝트를_생성한다() {
        Team team = team(10L);
        User creator = user(1L, "creator");
        User leader = user(2L, "leader");
        when(teamRepository.findByIdAndMemberUserId(10L, 1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findAllWithUserByTeamId(10L))
                .thenReturn(List.of(teamMember(team, creator), teamMember(team, leader)));
        when(repositoryLinkRepository.existsByGithubRepositoryId(1001L)).thenReturn(false);
        when(githubClient.getRepositorySnapshot(500L, 1001L)).thenReturn(
                new GithubRepositorySnapshot(new GithubRepositoryMetadata(
                        1001L,
                        500L,
                        "Team-dAIving",
                        "FlowMate-BE",
                        "https://github.com/Team-dAIving/FlowMate-BE"
                ), List.of(new GithubIssueMetadata(
                        17,
                        "로그인 API 구현",
                        "OAuth 로그인을 구현합니다.",
                        "open",
                        "creator",
                        null
                )), true)
        );
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            assertThat(project.getDescription()).isEqualTo("프로젝트 관리 서비스");
            assertThat(project.getDesignText()).isEqualTo("사용자 인증과 업무 관리를 중심으로 설계합니다.");
            assertThat(project.getSubject()).isEqualTo("MVP 출시");
            project.setId(20L);
            return project;
        });
        when(repositoryLinkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(1L, validRequest());

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.description()).isEqualTo("프로젝트 관리 서비스");
        assertThat(response.goal()).isEqualTo("MVP 출시");
        assertThat(response.designText()).isEqualTo("사용자 인증과 업무 관리를 중심으로 설계합니다.");
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.importedGithubIssueCount()).isEqualTo(1);
        assertThat(response.githubIssueImportStatus()).isEqualTo("SUCCESS");
        assertThat(response.repositories()).singleElement().satisfies(repository -> {
            assertThat(repository.githubRepositoryId()).isEqualTo(1001L);
            assertThat(repository.owner()).isEqualTo("Team-dAIving");
            assertThat(repository.name()).isEqualTo("FlowMate-BE");
            assertThat(repository.role()).isEqualTo("BACKEND");
        });
        verify(githubClient).getRepositorySnapshot(500L, 1001L);
        verify(projectMemberRepository).saveAll(any());
        verify(taskRepository).saveAll(any());
    }

    @Test
    void 팀에_속하지_않은_사용자는_프로젝트_멤버로_추가할_수_없다() {
        Team team = team(10L);
        User creator = user(1L, "creator");
        when(teamRepository.findByIdAndMemberUserId(10L, 1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findAllWithUserByTeamId(10L))
                .thenReturn(List.of(teamMember(team, creator)));

        assertThatThrownBy(() -> service.create(1L, validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("팀에 속하지 않은 사용자");
        verify(githubClient, never()).getRepositorySnapshot(any(), any());
        verify(projectRepository, never()).save(any());
    }

    private CreateProjectRequest validRequest() {
        return new CreateProjectRequest(
                "FlowMate",
                "프로젝트 관리 서비스",
                "MVP 출시",
                "사용자 인증과 업무 관리를 중심으로 설계합니다.",
                "web",
                LocalDate.of(2026, 11, 23),
                LocalDate.of(2026, 12, 23),
                10L,
                2L,
                List.of(
                        new CreateProjectRequest.Member(
                                1L,
                                "backend",
                                List.of("Java", "Spring Boot")
                        ),
                        new CreateProjectRequest.Member(
                                2L,
                                "frontend",
                                List.of("TypeScript", "React")
                        )
                ),
                List.of("Docker"),
                List.of(new CreateProjectRequest.Repository(500L, 1001L, "backend"))
        );
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("팀");
        return team;
    }

    private User user(Long id, String login) {
        User user = new User();
        user.setId(id);
        user.setGithubLogin(login);
        return user;
    }

    private TeamMember teamMember(Team team, User user) {
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        return member;
    }
}
