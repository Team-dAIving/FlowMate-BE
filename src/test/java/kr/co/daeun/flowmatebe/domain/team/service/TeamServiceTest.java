package kr.co.daeun.flowmatebe.domain.team.service;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.team.dto.request.CreateTeamRequest;
import kr.co.daeun.flowmatebe.domain.team.dto.response.CreateTeamResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamListResponse;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamMemberRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TeamService teamService = new TeamService(
            teamRepository,
            teamMemberRepository,
            projectRepository,
            taskRepository,
            userRepository
    );

    @Test
    void 로그인_사용자가_속한_팀만_조회한다() {
        when(teamRepository.findAllByMemberUserIdOrderByIdAsc(7L)).thenReturn(List.of());

        TeamListResponse response = teamService.getTeams(7L);

        assertThat(response.teams()).isEmpty();
        verify(teamRepository).findAllByMemberUserIdOrderByIdAsc(7L);
    }

    @Test
    void 팀_생성자는_팀원_요청에_없어도_자동으로_포함된다() {
        User creator = user(7L);
        when(userRepository.findAllById(any())).thenReturn(List.of(creator));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(10L);
            return team;
        });
        when(teamMemberRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTeamResponse response = teamService.createTeam(
                7L,
                new CreateTeamRequest(" Team ", null, "#ab12ef", List.of())
        );

        ArgumentCaptor<List<TeamMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(teamMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue())
                .extracting(member -> member.getUser().getId())
                .containsExactly(7L);
        assertThat(response.memberCount()).isEqualTo(1);
        assertThat(response.themeColor()).isEqualTo("#AB12EF");
    }

    @Test
    void 팀_상세는_프로젝트_진행률과_팀원별_업무_수를_계산한다() {
        Team team = team(10L);
        User memberUser = user(7L);
        TeamMember member = teamMember(team, memberUser);
        Project project = project(20L, team);

        Task todo = task(100L, project, memberUser, "TODO");
        Task inProgress = task(101L, project, memberUser, "IN_PROGRESS");
        Task done = task(102L, project, memberUser, "DONE");

        when(teamRepository.findByIdAndMemberUserId(10L, 7L)).thenReturn(Optional.of(team));
        when(projectRepository.findAllByTeam_IdOrderByCreatedAtAsc(10L)).thenReturn(List.of(project));
        when(teamMemberRepository.findAllWithUserByTeamId(10L)).thenReturn(List.of(member));
        when(taskRepository.findAllWithDetailsByTeamId(10L)).thenReturn(List.of(todo, inProgress, done));

        var response = teamService.getTeamDetail(7L, 10L);

        assertThat(response.projectCount()).isEqualTo(1);
        assertThat(response.memberCount()).isEqualTo(1);
        assertThat(response.taskCount()).isEqualTo(3);
        assertThat(response.inProgressTaskCount()).isEqualTo(2);
        assertThat(response.completedTaskCount()).isEqualTo(1);
        assertThat(response.projects().get(0).progressPercent()).isEqualTo(33);
        assertThat(response.projects().get(0).totalTaskCount()).isEqualTo(3);
        assertThat(response.members().get(0).totalAssignedTaskCount()).isEqualTo(3);
        assertThat(response.members().get(0).inProgressAssignedTaskCount()).isEqualTo(1);
        assertThat(response.tasks()).extracting(taskResponse -> taskResponse.id())
                .containsExactly(100L, 101L, 102L);
    }

    @Test
    void 소속되지_않은_팀의_상세는_조회할_수_없다() {
        when(teamRepository.findByIdAndMemberUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamDetail(7L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verifyNoInteractions(projectRepository, taskRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setNickname("user-" + id);
        return user;
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("team-" + id);
        return team;
    }

    private TeamMember teamMember(Team team, User user) {
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        return member;
    }

    private Project project(Long id, Team team) {
        Project project = new Project();
        project.setId(id);
        project.setName("project-" + id);
        project.setTeam(team);
        return project;
    }

    private Task task(Long id, Project project, User assignee, String status) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.setProject(project);
        projectMember.setUser(assignee);

        Task task = new Task();
        task.setId(id);
        task.setProject(project);
        task.setAssigneeMember(projectMember);
        task.setStatus(status);
        task.setTitle("task-" + id);
        return task;
    }
}
