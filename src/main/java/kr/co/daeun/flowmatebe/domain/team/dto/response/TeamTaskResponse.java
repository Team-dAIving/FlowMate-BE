package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;

import java.time.LocalDate;

public record TeamTaskResponse(
        Long id,
        Long projectId,
        String projectName,
        String title,
        String status,
        String priority,
        Long assigneeUserId,
        String assigneeNickname,
        String assigneeAvatarUrl,
        LocalDate dueDate,
        Integer githubIssueNumber
) {

    public static TeamTaskResponse from(Task task) {
        ProjectMember assignee = task.getAssigneeMember();
        return new TeamTaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                assignee == null ? null : assignee.getUser().getId(),
                assignee == null ? null : assignee.getUser().getNickname(),
                assignee == null ? null : assignee.getUser().getAvatarUrl(),
                task.getDueDate(),
                task.getGithubIssueNumber()
        );
    }
}
