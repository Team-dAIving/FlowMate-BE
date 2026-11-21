package kr.co.daeun.flowmatebe.domain.team.service;

import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setNickname("user-" + id);
        return user;
    }
}
