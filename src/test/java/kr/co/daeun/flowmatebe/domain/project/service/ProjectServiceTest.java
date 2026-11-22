package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ProjectService projectService = new ProjectService(
            projectRepository,
            projectMemberRepository,
            taskRepository
    );

    @Test
    void 로그인_사용자가_참여한_프로젝트와_리더_및_진행률을_반환한다() {
        Team team = team(10L, "플로우메이트 팀");
        Project project = project(20L, team, "WEB");
        ProjectMember leader = member(30L, project, user(1L, "팀장"), "Leader");
        ProjectMember developer = member(31L, project, user(2L, "개발자"), "Member");
        TaskRepository.ProjectTaskCount taskCount = mock(TaskRepository.ProjectTaskCount.class);

        when(projectRepository.findAllByMemberUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(project));
        when(projectMemberRepository.findAllWithUserByProjectIds(List.of(20L)))
                .thenReturn(List.of(leader, developer));
        when(taskRepository.countTasksByProjectIds(List.of(20L), "DONE"))
                .thenReturn(List.of(taskCount));
        when(taskCount.getProjectId()).thenReturn(20L);
        when(taskCount.getTotalCount()).thenReturn(9L);
        when(taskCount.getCompletedCount()).thenReturn(2L);

        var response = projectService.getMyProjects(2L);

        assertThat(response.projects()).hasSize(1);
        var item = response.projects().get(0);
        assertThat(item.id()).isEqualTo(20L);
        assertThat(item.type()).isEqualTo("WEB");
        assertThat(item.teamName()).isEqualTo("플로우메이트 팀");
        assertThat(item.leaderName()).isEqualTo("팀장");
        assertThat(item.totalTaskCount()).isEqualTo(9);
        assertThat(item.completedTaskCount()).isEqualTo(2);
        assertThat(item.progressPercent()).isEqualTo(22);
        assertThat(item.members()).extracting(member -> member.userId())
                .containsExactly(1L, 2L);
        verify(projectRepository).findAllByMemberUserIdOrderByCreatedAtDesc(2L);
    }

    @Test
    void 업무가_없는_프로젝트의_진행률은_0이다() {
        Project project = project(20L, team(10L, "팀"), "OTHER");
        when(projectRepository.findAllByMemberUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(project));
        when(projectMemberRepository.findAllWithUserByProjectIds(List.of(20L)))
                .thenReturn(List.of());
        when(taskRepository.countTasksByProjectIds(List.of(20L), "DONE"))
                .thenReturn(List.of());

        var item = projectService.getMyProjects(1L).projects().get(0);

        assertThat(item.totalTaskCount()).isZero();
        assertThat(item.completedTaskCount()).isZero();
        assertThat(item.progressPercent()).isZero();
        assertThat(item.leaderName()).isNull();
    }

    @Test
    void 참여한_프로젝트가_없으면_빈_목록을_반환한다() {
        when(projectRepository.findAllByMemberUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        var response = projectService.getMyProjects(1L);

        assertThat(response.projects()).isEmpty();
        verifyNoInteractions(projectMemberRepository, taskRepository);
    }

    private Project project(Long id, Team team, String type) {
        Project project = new Project();
        project.setId(id);
        project.setName("프로젝트-" + id);
        project.setDescription("설명");
        project.setType(type);
        project.setTeam(team);
        project.setTechStack(Map.of("languages", List.of("Java", "TypeScript")));
        return project;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        team.setThemeColor("#4F46E5");
        return team;
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
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
        return member;
    }
}
