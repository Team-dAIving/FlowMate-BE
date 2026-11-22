package kr.co.daeun.flowmatebe.domain.task.dto.response;

import kr.co.daeun.flowmatebe.domain.task.entity.Task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskDetailResponse(
        Long id,
        Long projectId,
        String projectName,
        String title,
        String description,
        String status,
        String priority,
        String planningStatus,
        LocalDate startDate,
        LocalDate dueDate,
        Integer estimatedTime,
        Integer actualTime,
        Instant completedAt,
        TaskAssigneeResponse assignee,
        TaskGithubResponse github,
        List<TaskCommentResponse> comments,
        Instant createdAt,
        Instant updatedAt
) {

    public static TaskDetailResponse of(Task task, List<TaskCommentResponse> comments) {
        return new TaskDetailResponse(
                task.getId(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getPlanningStatus(),
                task.getStartDate(),
                task.getDueDate(),
                task.getEstimatedTime(),
                task.getActualTime(),
                task.getCompletedAt(),
                TaskAssigneeResponse.from(task.getAssigneeMember()),
                TaskGithubResponse.from(task),
                comments,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
