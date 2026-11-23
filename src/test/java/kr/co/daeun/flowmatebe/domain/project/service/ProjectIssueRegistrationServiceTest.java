package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.request.RegisterIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepositoryLinkRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.global.github.dto.GithubCreatedIssue;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectIssueRegistrationServiceTest {

    private final kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository projectRepository =
            mock(kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final ProjectRepositoryLinkRepository repositoryLinkRepository =
            mock(ProjectRepositoryLinkRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final GithubInstallationClient githubClient = mock(GithubInstallationClient.class);
    private final ProjectIssueRegistrationService service = new ProjectIssueRegistrationService(
            projectRepository,
            projectMemberRepository,
            repositoryLinkRepository,
            taskRepository,
            githubClient
    );

    @Test
    void 수정한_초안을_업무로_등록하고_선택한_항목만_GitHub_이슈로_생성한다() {
        Project project = project();
        ProjectMember backendMember = member(project, 7L, "backend");
        ProjectRepository backendRepository = repository(project, 1001L, "BACKEND");
        stubProject(project, backendMember, backendRepository);
        assignTaskIdsOnSave();
        when(githubClient.createIssue(
                500L,
                "Team-dAIving",
                "FlowMate-BE",
                "로그인 API 구현",
                "JWT 로그인을 구현합니다.",
                List.of("feature", "backend")
        )).thenReturn(new GithubCreatedIssue(
                31,
                "https://github.com/Team-dAIving/FlowMate-BE/issues/31"
        ));

        var response = service.register(1L, 20L, new RegisterIssueDraftsRequest(List.of(
                issue("로그인 API 구현", true, 7L, 8.5, List.of("feature", "backend")),
                issue("로그인 화면 구현", false, null, null, List.of())
        )));

        assertThat(response.createdTaskCount()).isEqualTo(2);
        assertThat(response.createdGithubIssueCount()).isEqualTo(1);
        assertThat(response.failedGithubIssueCount()).isZero();
        assertThat(response.tasks()).extracting(task -> task.githubSyncStatus())
                .containsExactly("SYNCED", "NOT_LINKED");
        assertThat(response.tasks().get(0).githubIssueNumber()).isEqualTo(31);
        assertThat(response.tasks().get(0).githubRepositoryId()).isEqualTo(1001L);
        assertThat(response.tasks().get(1).githubIssueNumber()).isNull();
        verify(githubClient).createIssue(
                500L,
                "Team-dAIving",
                "FlowMate-BE",
                "로그인 API 구현",
                "JWT 로그인을 구현합니다.",
                List.of("feature", "backend")
        );
        verify(taskRepository, org.mockito.Mockito.times(2)).saveAll(any());
    }

    @Test
    void GitHub_이슈_생성_실패는_업무에_FAILED로_기록한다() {
        Project project = project();
        ProjectMember backendMember = member(project, 7L, "backend");
        ProjectRepository backendRepository = repository(project, 1001L, "BACKEND");
        stubProject(project, backendMember, backendRepository);
        assignTaskIdsOnSave();
        when(githubClient.createIssue(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "GitHub App에 이슈 생성 권한이 없습니다."
                ));

        var response = service.register(1L, 20L, new RegisterIssueDraftsRequest(List.of(
                issue("로그인 API 구현", true, 7L, 2.0, List.of())
        )));

        assertThat(response.createdTaskCount()).isEqualTo(1);
        assertThat(response.createdGithubIssueCount()).isZero();
        assertThat(response.failedGithubIssueCount()).isEqualTo(1);
        assertThat(response.tasks().get(0).githubSyncStatus()).isEqualTo("FAILED");
        assertThat(response.tasks().get(0).githubError())
                .isEqualTo("GitHub App에 이슈 생성 권한이 없습니다.");
    }

    @Test
    void 프로젝트_멤버가_아닌_담당자는_업무를_저장하지_않는다() {
        Project project = project();
        when(projectRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findAllWithUserByProjectId(20L)).thenReturn(List.of());
        when(repositoryLinkRepository.findAllByProject_IdOrderByIdAsc(20L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.register(
                1L,
                20L,
                new RegisterIssueDraftsRequest(List.of(
                        issue("로그인 API 구현", false, 999L, 2.0, List.of())
                ))
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("프로젝트 멤버가 아닌 담당자");

        verify(taskRepository, never()).saveAll(any());
        verify(githubClient, never()).createIssue(any(), any(), any(), any(), any(), any());
    }

    private void stubProject(
            Project project,
            ProjectMember member,
            ProjectRepository repository
    ) {
        when(projectRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findAllWithUserByProjectId(20L))
                .thenReturn(List.of(member));
        when(repositoryLinkRepository.findAllByProject_IdOrderByIdAsc(20L))
                .thenReturn(List.of(repository));
    }

    private void assignTaskIdsOnSave() {
        AtomicLong sequence = new AtomicLong(100L);
        when(taskRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Task> tasks = invocation.getArgument(0);
            assertThat(tasks).allSatisfy(task ->
                    assertThat(task.getPlanningStatus()).isEqualTo("CONFIRMED")
            );
            tasks.stream()
                    .filter(task -> task.getId() == null)
                    .forEach(task -> task.setId(sequence.getAndIncrement()));
            return tasks;
        });
    }

    private Project project() {
        Project project = new Project();
        project.setId(20L);
        project.setStartDate(LocalDate.of(2026, 11, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        return project;
    }

    private ProjectMember member(Project project, Long userId, String part) {
        User user = new User();
        user.setId(userId);
        ProjectMember member = new ProjectMember();
        member.setId(70L);
        member.setProject(project);
        member.setUser(user);
        member.setParts(List.of(part));
        return member;
    }

    private ProjectRepository repository(Project project, Long githubRepositoryId, String role) {
        ProjectRepository repository = new ProjectRepository();
        repository.setId(80L);
        repository.setProject(project);
        repository.setGithubRepositoryId(githubRepositoryId);
        repository.setGithubInstallationId(500L);
        repository.setOwnerLogin("Team-dAIving");
        repository.setRepositoryName("FlowMate-BE");
        repository.setRepositoryRole(role);
        repository.setHtmlUrl("https://github.com/Team-dAIving/FlowMate-BE");
        return repository;
    }

    private RegisterIssueDraftsRequest.Issue issue(
            String title,
            boolean createGithubIssue,
            Long assigneeId,
            Double estimatedHours,
            List<String> labels
    ) {
        return new RegisterIssueDraftsRequest.Issue(
                title,
                title.equals("로그인 API 구현")
                        ? "JWT 로그인을 구현합니다."
                        : "로그인 화면을 구현합니다.",
                "HIGH",
                assigneeId,
                LocalDate.of(2026, 11, 24),
                LocalDate.of(2026, 11, 28),
                estimatedHours,
                labels,
                createGithubIssue,
                null
        );
    }
}
