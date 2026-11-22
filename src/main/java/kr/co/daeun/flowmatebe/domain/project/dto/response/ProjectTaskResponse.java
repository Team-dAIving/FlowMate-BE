package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectTaskResponse(
        Long id,
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
        Instant createdAt,
        Instant updatedAt,
        Assignee assignee,
        Github github,
        long commentCount
) {

    public static ProjectTaskResponse of(Task task, long commentCount) {
        return new ProjectTaskResponse(
                task.getId(),
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
                task.getCreatedAt(),
                task.getUpdatedAt(),
                Assignee.from(task.getAssigneeMember()),
                Github.from(task.getGithubRepository(), task.getGithubIssueNumber()),
                commentCount
        );
    }

    public record Assignee(
            Long projectMemberId,
            Long userId,
            String nickname,
            String avatarUrl
    ) {

        private static Assignee from(ProjectMember member) {
            if (member == null) {
                return null;
            }
            return new Assignee(
                    member.getId(),
                    member.getUser().getId(),
                    member.getUser().getNickname(),
                    member.getUser().getAvatarUrl()
            );
        }
    }

    public record Github(
            Long projectRepositoryId,
            Long githubRepositoryId,
            String repositoryName,
            String htmlUrl,
            Integer issueNumber
    ) {

        private static Github from(ProjectRepository repository, Integer issueNumber) {
            if (repository == null && issueNumber == null) {
                return null;
            }
            return new Github(
                    repository == null ? null : repository.getId(),
                    repository == null ? null : repository.getGithubRepositoryId(),
                    repository == null ? null : repository.getRepositoryName(),
                    repository == null ? null : repository.getHtmlUrl(),
                    issueNumber
            );
        }
    }
}
