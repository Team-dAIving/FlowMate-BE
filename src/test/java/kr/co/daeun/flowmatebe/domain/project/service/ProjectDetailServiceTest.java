package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingTaskSuggestionRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepositoryLinkRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskCommentRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectDetailServiceTest {

    private final kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository projectRepository =
            mock(kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final ProjectRepositoryLinkRepository repositoryLinkRepository =
            mock(ProjectRepositoryLinkRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskCommentRepository taskCommentRepository = mock(TaskCommentRepository.class);
    private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
    private final MeetingTaskSuggestionRepository suggestionRepository =
            mock(MeetingTaskSuggestionRepository.class);
    private final ProjectDetailService service = new ProjectDetailService(
            projectRepository,
            projectMemberRepository,
            repositoryLinkRepository,
            taskRepository,
            taskCommentRepository,
            meetingRepository,
            suggestionRepository
    );

    @Test
    void 프로젝트_상세에_공통_저장소와_대시보드_집계를_반환한다() {
        Project project = project(20L);
        ProjectMember leader = member(30L, project, user(1L, "리더"), "Leader");
        ProjectMember developer = member(31L, project, user(2L, "개발자"), "Member");
        Task todo = task(100L, "TODO", developer);
        Task inProgress = task(101L, "IN_PROGRESS", leader);
        Task done = task(102L, "DONE", developer);
        done.setCompletedAt(Instant.now());
        Task cancelled = task(103L, "CANCELLED", null);
        ProjectRepository commonRepository = commonRepository(project);
        TaskCommentRepository.TaskCommentCount commentCount =
                mock(TaskCommentRepository.TaskCommentCount.class);

        when(projectRepository.findAccessibleByIdAndUserId(20L, 2L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findAllWithUserByProjectId(20L))
                .thenReturn(List.of(leader, developer));
        when(taskRepository.findAllWithDetailsByProjectId(20L))
                .thenReturn(List.of(todo, inProgress, done, cancelled));
        when(meetingRepository.countByProject_Id(20L)).thenReturn(2L);
        when(repositoryLinkRepository
                .findFirstByProject_IdAndRepositoryRoleIgnoreCaseOrderByIdAsc(20L, "COMMON"))
                .thenReturn(Optional.of(commonRepository));
        when(taskCommentRepository.countByTaskIds(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(commentCount));
        when(commentCount.getTaskId()).thenReturn(100L);
        when(commentCount.getCommentCount()).thenReturn(3L);

        var response = service.getProjectDetail(2L, 20L);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.leader().nickname()).isEqualTo("리더");
        assertThat(response.commonRepository().repositoryName()).isEqualTo("flowmate-common");
        assertThat(response.counts().taskCount()).isEqualTo(4);
        assertThat(response.counts().meetingCount()).isEqualTo(2);
        assertThat(response.counts().memberCount()).isEqualTo(2);
        assertThat(response.progress().completedTaskCount()).isEqualTo(1);
        assertThat(response.progress().percent()).isEqualTo(25);
        assertThat(response.taskStatusCounts()).containsExactly(
                org.assertj.core.data.MapEntry.entry("TODO", 1L),
                org.assertj.core.data.MapEntry.entry("IN_PROGRESS", 1L),
                org.assertj.core.data.MapEntry.entry("DONE", 1L),
                org.assertj.core.data.MapEntry.entry("CANCELLED", 1L)
        );
        assertThat(response.workload())
                .filteredOn(workload -> workload.userId().equals(2L))
                .singleElement()
                .satisfies(workload -> {
                    assertThat(workload.activeTaskCount()).isEqualTo(1);
                    assertThat(workload.completedTaskCount()).isEqualTo(1);
                });
        assertThat(response.velocity()).hasSize(7);
        assertThat(response.velocity().get(6).date())
                .isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
        assertThat(response.velocity().get(6).completedTaskCount()).isEqualTo(1);
        assertThat(response.recentTasks()).hasSize(3);
        assertThat(response.recentTasks().get(0).commentCount()).isEqualTo(3);
    }

    @Test
    void 회의의_승인된_suggestion만_Action_Item으로_집계한다() {
        Project project = project(20L);
        Meeting first = meeting(40L, project, "주간 회의");
        Meeting second = meeting(41L, project, "리뷰 회의");
        MeetingTaskSuggestionRepository.MeetingSuggestionCount approvedCount =
                mock(MeetingTaskSuggestionRepository.MeetingSuggestionCount.class);

        when(projectRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(project));
        when(meetingRepository.findAllByProject_IdOrderByHeldAtDescIdDesc(20L))
                .thenReturn(List.of(first, second));
        when(suggestionRepository.countByMeetingIdsAndReviewStatus(
                List.of(40L, 41L),
                "APPROVED"
        )).thenReturn(List.of(approvedCount));
        when(approvedCount.getMeetingId()).thenReturn(40L);
        when(approvedCount.getSuggestionCount()).thenReturn(2L);

        var response = service.getProjectMeetings(1L, 20L);

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.meetings().get(0).approvedActionItemCount()).isEqualTo(2);
        assertThat(response.meetings().get(1).approvedActionItemCount()).isZero();
    }

    @Test
    void 리더는_OWNER_나머지는_MEMBER로_반환한다() {
        Project project = project(20L);
        ProjectMember leader = member(30L, project, user(1L, "리더"), "Leader");
        ProjectMember developer = member(31L, project, user(2L, "개발자"), "Member");
        when(projectRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findAllWithUserByProjectId(20L))
                .thenReturn(List.of(leader, developer));

        var response = service.getProjectMembers(1L, 20L);

        assertThat(response.members()).extracting(member -> member.accessRole())
                .containsExactly("OWNER", "MEMBER");
        assertThat(response.members().get(0).leader()).isTrue();
        assertThat(response.members().get(1).leader()).isFalse();
    }

    @Test
    void 프로젝트에_연결된_GitHub_저장소_목록을_반환한다() {
        Project project = project(20L);
        ProjectRepository repository = commonRepository(project);
        when(projectRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(project));
        when(repositoryLinkRepository.findAllByProject_IdOrderByIdAsc(20L))
                .thenReturn(List.of(repository));

        var response = service.getProjectRepositories(1L, 20L);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.repositories()).singleElement().satisfies(item -> {
            assertThat(item.githubRepositoryId()).isEqualTo(500L);
            assertThat(item.owner()).isEqualTo("flowmate");
            assertThat(item.name()).isEqualTo("flowmate-common");
            assertThat(item.role()).isEqualTo("COMMON");
        });
    }

    @Test
    void 프로젝트_멤버가_아니면_상세_리소스를_노출하지_않는다() {
        when(projectRepository.findAccessibleByIdAndUserId(20L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProjectTasks(99L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception).getStatusCode()
                ).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(taskRepository, taskCommentRepository);
    }

    private Project project(Long id) {
        Team team = new Team();
        team.setId(10L);
        team.setName("플로우메이트");
        team.setThemeColor("#4F46E5");
        Project project = new Project();
        project.setId(id);
        project.setTeam(team);
        project.setName("프로젝트");
        project.setDescription("설명");
        project.setSubject("목표");
        project.setType("AI");
        project.setStatus("IN_PROGRESS");
        return project;
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setEmail("user" + id + "@example.com");
        user.setAvatarUrl("https://example.com/avatar/" + id);
        return user;
    }

    private ProjectMember member(
            Long id,
            Project project,
            User user,
            String role
    ) {
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        member.setParts(List.of("backend"));
        member.setTechStack(List.of("Spring Boot"));
        return member;
    }

    private Task task(Long id, String status, ProjectMember assignee) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("업무-" + id);
        task.setStatus(status);
        task.setPriority("MEDIUM");
        task.setAssigneeMember(assignee);
        return task;
    }

    private ProjectRepository commonRepository(Project project) {
        ProjectRepository repository = new ProjectRepository();
        repository.setId(50L);
        repository.setProject(project);
        repository.setGithubRepositoryId(500L);
        repository.setOwnerLogin("flowmate");
        repository.setRepositoryName("flowmate-common");
        repository.setRepositoryRole("COMMON");
        repository.setHtmlUrl("https://github.com/flowmate/flowmate-common");
        return repository;
    }

    private Meeting meeting(Long id, Project project, String title) {
        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setProject(project);
        meeting.setTitle(title);
        meeting.setProcessingStatus("COMPLETED");
        return meeting;
    }
}
