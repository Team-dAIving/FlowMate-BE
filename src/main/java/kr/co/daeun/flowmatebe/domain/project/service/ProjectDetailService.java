package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingTaskSuggestionRepository;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectDetailResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMeetingsResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMembersResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectRepositoriesResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectTaskResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectTasksResponse;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepositoryLinkRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskCommentRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectDetailService {

    private static final String LEADER_ROLE = "Leader";
    private static final String OWNER_ACCESS_ROLE = "OWNER";
    private static final String MEMBER_ACCESS_ROLE = "MEMBER";
    private static final String COMMON_REPOSITORY_ROLE = "COMMON";
    private static final String APPROVED_REVIEW_STATUS = "APPROVED";
    private static final String DONE_STATUS = "DONE";
    private static final String CANCELLED_STATUS = "CANCELLED";
    private static final List<String> TASK_STATUSES = List.of(
            "TODO",
            "IN_PROGRESS",
            DONE_STATUS,
            CANCELLED_STATUS
    );
    private static final ZoneId PROJECT_ZONE = ZoneId.of("Asia/Seoul");
    private static final int RECENT_TASK_LIMIT = 3;
    private static final int VELOCITY_DAYS = 7;

    private final kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepositoryLinkRepository repositoryLinkRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingTaskSuggestionRepository meetingTaskSuggestionRepository;

    public ProjectDetailResponse getProjectDetail(Long currentUserId, Long projectId) {
        Project project = getAccessibleProject(currentUserId, projectId);
        List<ProjectMember> members = projectMemberRepository.findAllWithUserByProjectId(projectId);
        List<Task> tasks = taskRepository.findAllWithDetailsByProjectId(projectId);
        long meetingCount = meetingRepository.countByProject_Id(projectId);
        ProjectRepository commonRepository = repositoryLinkRepository
                .findFirstByProject_IdAndRepositoryRoleIgnoreCaseOrderByIdAsc(
                        projectId,
                        COMMON_REPOSITORY_ROLE
                )
                .orElse(null);

        long completedTaskCount = tasks.stream()
                .filter(task -> isStatus(task, DONE_STATUS))
                .count();
        int progressPercent = tasks.isEmpty()
                ? 0
                : (int) Math.round(completedTaskCount * 100.0 / tasks.size());
        List<Task> recentTasks = tasks.stream()
                .limit(RECENT_TASK_LIMIT)
                .toList();
        Map<Long, Long> recentCommentCounts = commentCounts(recentTasks);

        return new ProjectDetailResponse(
                project.getId(),
                project.getType(),
                project.getName(),
                project.getDescription(),
                project.getSubject(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                toLeader(members),
                new ProjectDetailResponse.Team(
                        project.getTeam().getId(),
                        project.getTeam().getName(),
                        project.getTeam().getThemeColor()
                ),
                toCommonRepository(commonRepository),
                new ProjectDetailResponse.Counts(
                        tasks.size(),
                        meetingCount,
                        members.size()
                ),
                new ProjectDetailResponse.Progress(
                        tasks.size(),
                        completedTaskCount,
                        progressPercent
                ),
                statusCounts(tasks),
                workloads(members, tasks),
                velocity(tasks),
                recentTasks.stream()
                        .map(task -> ProjectTaskResponse.of(
                                task,
                                recentCommentCounts.getOrDefault(task.getId(), 0L)
                        ))
                        .toList()
        );
    }

    public ProjectTasksResponse getProjectTasks(Long currentUserId, Long projectId) {
        getAccessibleProject(currentUserId, projectId);
        List<Task> tasks = taskRepository.findAllWithDetailsByProjectId(projectId);
        Map<Long, Long> commentCounts = commentCounts(tasks);
        return new ProjectTasksResponse(tasks.stream()
                .map(task -> ProjectTaskResponse.of(
                        task,
                        commentCounts.getOrDefault(task.getId(), 0L)
                ))
                .toList());
    }

    public ProjectMeetingsResponse getProjectMeetings(Long currentUserId, Long projectId) {
        getAccessibleProject(currentUserId, projectId);
        List<Meeting> meetings = meetingRepository
                .findAllByProject_IdOrderByHeldAtDescIdDesc(projectId);
        Map<Long, Long> approvedSuggestionCounts = approvedSuggestionCounts(meetings);
        return new ProjectMeetingsResponse(meetings.stream()
                .map(meeting -> new ProjectMeetingsResponse.Meeting(
                        meeting.getId(),
                        meeting.getTitle(),
                        meeting.getSummary(),
                        meeting.getHeldAt(),
                        meeting.getDurationSeconds(),
                        meeting.getProcessingStatus(),
                        approvedSuggestionCounts.getOrDefault(meeting.getId(), 0L)
                ))
                .toList());
    }

    public ProjectMembersResponse getProjectMembers(Long currentUserId, Long projectId) {
        getAccessibleProject(currentUserId, projectId);
        List<ProjectMembersResponse.Member> members = projectMemberRepository
                .findAllWithUserByProjectId(projectId)
                .stream()
                .map(member -> {
                    boolean leader = isLeader(member);
                    return new ProjectMembersResponse.Member(
                            member.getId(),
                            member.getUser().getId(),
                            member.getUser().getNickname(),
                            member.getUser().getEmail(),
                            member.getUser().getAvatarUrl(),
                            copyOrEmpty(member.getParts()),
                            copyOrEmpty(member.getTechStack()),
                            leader,
                            leader ? OWNER_ACCESS_ROLE : MEMBER_ACCESS_ROLE
                    );
                })
                .toList();
        return new ProjectMembersResponse(members);
    }

    public ProjectRepositoriesResponse getProjectRepositories(
            Long currentUserId,
            Long projectId
    ) {
        getAccessibleProject(currentUserId, projectId);
        return new ProjectRepositoriesResponse(repositoryLinkRepository
                .findAllByProject_IdOrderByIdAsc(projectId)
                .stream()
                .map(ProjectRepositoriesResponse.Repository::from)
                .toList());
    }

    private Project getAccessibleProject(Long currentUserId, Long projectId) {
        return projectRepository.findAccessibleByIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "접근 가능한 프로젝트를 찾을 수 없습니다."
                ));
    }

    private ProjectDetailResponse.Leader toLeader(List<ProjectMember> members) {
        return members.stream()
                .filter(this::isLeader)
                .findFirst()
                .map(member -> new ProjectDetailResponse.Leader(
                        member.getUser().getId(),
                        member.getUser().getNickname(),
                        member.getUser().getAvatarUrl()
                ))
                .orElse(null);
    }

    private ProjectDetailResponse.CommonRepository toCommonRepository(ProjectRepository repository) {
        if (repository == null) {
            return null;
        }
        return new ProjectDetailResponse.CommonRepository(
                repository.getId(),
                repository.getGithubRepositoryId(),
                repository.getOwnerLogin(),
                repository.getRepositoryName(),
                repository.getHtmlUrl()
        );
    }

    private Map<String, Long> statusCounts(List<Task> tasks) {
        Map<String, Long> counts = new LinkedHashMap<>();
        TASK_STATUSES.forEach(status -> counts.put(status, 0L));
        tasks.stream()
                .map(Task::getStatus)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(counts::containsKey)
                .forEach(status -> counts.compute(status, (ignored, count) -> count + 1));
        return Collections.unmodifiableMap(counts);
    }

    private List<ProjectDetailResponse.Workload> workloads(
            List<ProjectMember> members,
            List<Task> tasks
    ) {
        Map<Long, MemberTaskCounts> countsByMemberId = new LinkedHashMap<>();
        tasks.stream()
                .filter(task -> task.getAssigneeMember() != null)
                .forEach(task -> {
                    Long memberId = task.getAssigneeMember().getId();
                    MemberTaskCounts counts = countsByMemberId.computeIfAbsent(
                            memberId,
                            ignored -> new MemberTaskCounts()
                    );
                    if (isStatus(task, DONE_STATUS)) {
                        counts.completed++;
                    } else if (!isStatus(task, CANCELLED_STATUS)) {
                        counts.active++;
                    }
                });
        return members.stream()
                .map(member -> {
                    MemberTaskCounts counts = countsByMemberId.getOrDefault(
                            member.getId(),
                            new MemberTaskCounts()
                    );
                    return new ProjectDetailResponse.Workload(
                            member.getId(),
                            member.getUser().getId(),
                            member.getUser().getNickname(),
                            member.getUser().getAvatarUrl(),
                            counts.active,
                            counts.completed
                    );
                })
                .toList();
    }

    private List<ProjectDetailResponse.Velocity> velocity(List<Task> tasks) {
        LocalDate today = LocalDate.now(PROJECT_ZONE);
        Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();
        for (int offset = VELOCITY_DAYS - 1; offset >= 0; offset--) {
            countsByDate.put(today.minusDays(offset), 0L);
        }
        tasks.stream()
                .filter(task -> isStatus(task, DONE_STATUS))
                .map(Task::getCompletedAt)
                .filter(Objects::nonNull)
                .map(completedAt -> completedAt.atZone(PROJECT_ZONE).toLocalDate())
                .filter(countsByDate::containsKey)
                .forEach(date -> countsByDate.compute(date, (ignored, count) -> count + 1));
        return countsByDate.entrySet().stream()
                .map(entry -> new ProjectDetailResponse.Velocity(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    private Map<Long, Long> commentCounts(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return Map.of();
        }
        return taskCommentRepository.countByTaskIds(tasks.stream().map(Task::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        TaskCommentRepository.TaskCommentCount::getTaskId,
                        TaskCommentRepository.TaskCommentCount::getCommentCount
                ));
    }

    private Map<Long, Long> approvedSuggestionCounts(List<Meeting> meetings) {
        if (meetings.isEmpty()) {
            return Map.of();
        }
        return meetingTaskSuggestionRepository.countByMeetingIdsAndReviewStatus(
                        meetings.stream().map(Meeting::getId).toList(),
                        APPROVED_REVIEW_STATUS
                )
                .stream()
                .collect(Collectors.toMap(
                        MeetingTaskSuggestionRepository.MeetingSuggestionCount::getMeetingId,
                        MeetingTaskSuggestionRepository.MeetingSuggestionCount::getSuggestionCount
                ));
    }

    private boolean isLeader(ProjectMember member) {
        return member.getRole() != null
                && member.getRole().equalsIgnoreCase(LEADER_ROLE);
    }

    private boolean isStatus(Task task, String status) {
        return task.getStatus() != null
                && task.getStatus().equalsIgnoreCase(status);
    }

    private String normalize(String value) {
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private List<String> copyOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static final class MemberTaskCounts {

        private long active;
        private long completed;
    }
}
