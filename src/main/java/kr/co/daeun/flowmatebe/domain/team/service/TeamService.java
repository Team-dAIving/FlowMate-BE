package kr.co.daeun.flowmatebe.domain.team.service;

import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.team.dto.request.CreateTeamRequest;
import kr.co.daeun.flowmatebe.domain.team.dto.response.CreateTeamResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamDetailMemberResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamDetailProjectResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamDetailResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamListResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamMemberResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamProjectResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamTaskResponse;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamMemberRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
    private static final String DONE_STATUS = "DONE";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TeamListResponse getTeams(Long currentUserId) {
        List<Team> teams = teamRepository.findAllByMemberUserIdOrderByIdAsc(currentUserId);
        if (teams.isEmpty()) {
            return new TeamListResponse(List.of());
        }

        List<Long> teamIds = teams.stream()
                .map(Team::getId)
                .toList();

        Map<Long, List<TeamMemberResponse>> membersByTeamId = getMembersByTeamId(teamIds);
        Map<Long, List<TeamProjectResponse>> projectsByTeamId = getProjectsByTeamId(teamIds);
        Map<Long, Long> taskCountsByTeamId = getInProgressTaskCountsByTeamId(teamIds);

        List<TeamResponse> responses = teams.stream()
                .map(team -> TeamResponse.of(
                        team,
                        projectsByTeamId.getOrDefault(team.getId(), List.of()),
                        membersByTeamId.getOrDefault(team.getId(), List.of()),
                        taskCountsByTeamId.getOrDefault(team.getId(), 0L)
                ))
                .toList();

        return new TeamListResponse(responses);
    }

    public TeamDetailResponse getTeamDetail(Long currentUserId, Long teamId) {
        Team team = teamRepository.findByIdAndMemberUserId(teamId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "팀을 찾을 수 없습니다."
                ));

        List<Project> projects = projectRepository.findAllByTeam_IdOrderByCreatedAtAsc(teamId);
        List<TeamMember> teamMembers = teamMemberRepository.findAllWithUserByTeamId(teamId);
        List<Task> tasks = taskRepository.findAllWithDetailsByTeamId(teamId);

        Map<Long, TaskCounts> taskCountsByProjectId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getProject().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), this::countTasks)
                ));
        Map<Long, TaskCounts> taskCountsByAssigneeUserId = tasks.stream()
                .filter(task -> task.getAssigneeMember() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getAssigneeMember().getUser().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), this::countTasks)
                ));

        List<TeamDetailProjectResponse> projectResponses = projects.stream()
                .map(project -> {
                    TaskCounts counts = taskCountsByProjectId.getOrDefault(
                            project.getId(),
                            TaskCounts.EMPTY
                    );
                    return TeamDetailProjectResponse.of(
                            project,
                            counts.total(),
                            counts.completed()
                    );
                })
                .toList();
        List<TeamDetailMemberResponse> memberResponses = teamMembers.stream()
                .map(teamMember -> {
                    TaskCounts counts = taskCountsByAssigneeUserId.getOrDefault(
                            teamMember.getUser().getId(),
                            TaskCounts.EMPTY
                    );
                    return TeamDetailMemberResponse.of(
                            teamMember,
                            counts.total(),
                            counts.inProgress()
                    );
                })
                .toList();
        List<TeamTaskResponse> taskResponses = tasks.stream()
                .map(TeamTaskResponse::from)
                .toList();
        TaskCounts teamTaskCounts = countTasks(tasks);

        return TeamDetailResponse.of(
                team,
                projectResponses,
                memberResponses,
                taskResponses,
                teamTaskCounts.total() - teamTaskCounts.completed(),
                teamTaskCounts.completed()
        );
    }

    @Transactional
    public CreateTeamResponse createTeam(Long currentUserId, CreateTeamRequest request) {
        List<Long> memberUserIds = request.memberUserIds();
        Set<Long> uniqueMemberUserIds = new LinkedHashSet<>(memberUserIds);
        if (uniqueMemberUserIds.size() != memberUserIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "중복된 팀원이 포함되어 있습니다."
            );
        }
        uniqueMemberUserIds.add(currentUserId);
        if (uniqueMemberUserIds.size() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "로그인 사용자를 포함해 한 팀에는 최대 100명까지 추가할 수 있습니다."
            );
        }

        Map<Long, User> usersById = userRepository.findAllById(uniqueMemberUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (!usersById.containsKey(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "인증된 사용자를 찾을 수 없습니다."
            );
        }
        List<Long> missingUserIds = uniqueMemberUserIds.stream()
                .filter(userId -> !usersById.containsKey(userId))
                .toList();
        if (!missingUserIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 사용자 ID가 포함되어 있습니다: " + missingUserIds
            );
        }

        Team team = new Team();
        team.setName(request.name().strip());
        team.setDescription(normalizeNullableText(request.description()));
        team.setThemeColor(request.themeColor().toUpperCase(Locale.ROOT));
        Team savedTeam = teamRepository.save(team);

        List<TeamMember> teamMembers = uniqueMemberUserIds.stream()
                .map(userId -> createTeamMember(savedTeam, usersById.get(userId)))
                .toList();
        List<TeamMember> savedTeamMembers = teamMemberRepository.saveAll(teamMembers);

        List<TeamMemberResponse> memberResponses = savedTeamMembers.stream()
                .map(TeamMemberResponse::from)
                .toList();
        return CreateTeamResponse.of(savedTeam, memberResponses);
    }

    private TeamMember createTeamMember(Team team, User user) {
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(user);
        return teamMember;
    }

    private String normalizeNullableText(String value) {
        return value == null ? null : value.strip();
    }

    private Map<Long, List<TeamMemberResponse>> getMembersByTeamId(Collection<Long> teamIds) {
        return teamMemberRepository.findAllWithUserByTeamIds(teamIds).stream()
                .collect(Collectors.groupingBy(
                        teamMember -> teamMember.getTeam().getId(),
                        Collectors.mapping(TeamMemberResponse::from, Collectors.toList())
                ));
    }

    private Map<Long, List<TeamProjectResponse>> getProjectsByTeamId(Collection<Long> teamIds) {
        return projectRepository.findAllByTeam_IdInOrderByCreatedAtAsc(teamIds).stream()
                .collect(Collectors.groupingBy(
                        project -> project.getTeam().getId(),
                        Collectors.mapping(TeamProjectResponse::from, Collectors.toList())
                ));
    }

    private Map<Long, Long> getInProgressTaskCountsByTeamId(Collection<Long> teamIds) {
        return taskRepository.countTasksByTeamIdsAndStatus(teamIds, IN_PROGRESS_STATUS).stream()
                .collect(Collectors.toMap(
                        TaskRepository.TeamTaskCount::getTeamId,
                        TaskRepository.TeamTaskCount::getTaskCount
                ));
    }

    private TaskCounts countTasks(List<Task> tasks) {
        long completed = tasks.stream()
                .filter(task -> statusEquals(task, DONE_STATUS))
                .count();
        long inProgress = tasks.stream()
                .filter(task -> statusEquals(task, IN_PROGRESS_STATUS))
                .count();
        return new TaskCounts(tasks.size(), completed, inProgress);
    }

    private boolean statusEquals(Task task, String expectedStatus) {
        return task.getStatus() != null && task.getStatus().equalsIgnoreCase(expectedStatus);
    }

    private record TaskCounts(long total, long completed, long inProgress) {

        private static final TaskCounts EMPTY = new TaskCounts(0, 0, 0);
    }
}
