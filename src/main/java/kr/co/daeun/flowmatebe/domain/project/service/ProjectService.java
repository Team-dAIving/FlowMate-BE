package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectListItemResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectListResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMemberSummaryResponse;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private static final String COMPLETED_STATUS = "DONE";
    private static final String LEADER_ROLE = "Leader";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;

    public ProjectListResponse getMyProjects(Long currentUserId) {
        List<Project> projects = projectRepository
                .findAllByMemberUserIdOrderByCreatedAtDesc(currentUserId);
        if (projects.isEmpty()) {
            return new ProjectListResponse(List.of());
        }

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .toList();
        Map<Long, List<ProjectMember>> membersByProjectId = projectMemberRepository
                .findAllWithUserByProjectIds(projectIds)
                .stream()
                .collect(Collectors.groupingBy(
                        member -> member.getProject().getId(),
                        Collectors.toList()
                ));
        Map<Long, TaskCounts> taskCountsByProjectId = taskRepository
                .countTasksByProjectIds(projectIds, COMPLETED_STATUS)
                .stream()
                .collect(Collectors.toMap(
                        TaskRepository.ProjectTaskCount::getProjectId,
                        count -> new TaskCounts(count.getTotalCount(), count.getCompletedCount())
                ));

        List<ProjectListItemResponse> responses = projects.stream()
                .map(project -> toResponse(
                        project,
                        membersByProjectId.getOrDefault(project.getId(), List.of()),
                        taskCountsByProjectId.getOrDefault(project.getId(), TaskCounts.EMPTY)
                ))
                .toList();
        return new ProjectListResponse(responses);
    }

    private ProjectListItemResponse toResponse(
            Project project,
            List<ProjectMember> projectMembers,
            TaskCounts taskCounts
    ) {
        String leaderName = projectMembers.stream()
                .filter(this::isLeader)
                .map(ProjectMember::getUser)
                .map(user -> user.getNickname())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        List<ProjectMemberSummaryResponse> members = projectMembers.stream()
                .map(ProjectMemberSummaryResponse::from)
                .toList();
        return ProjectListItemResponse.of(
                project,
                members,
                leaderName,
                taskCounts.total(),
                taskCounts.completed()
        );
    }

    private boolean isLeader(ProjectMember projectMember) {
        return projectMember.getRole() != null
                && projectMember.getRole().equalsIgnoreCase(LEADER_ROLE);
    }

    private record TaskCounts(long total, long completed) {

        private static final TaskCounts EMPTY = new TaskCounts(0, 0);
    }
}
