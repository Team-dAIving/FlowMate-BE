package kr.co.daeun.flowmatebe.domain.task.dto.response;

import kr.co.daeun.flowmatebe.domain.project.dto.response.RegisterIssueDraftsResponse;

public record CreateTaskResponse(
        Long taskId,
        String title,
        boolean githubIssueRequested,
        String githubSyncStatus,
        Long githubRepositoryId,
        Integer githubIssueNumber,
        String githubIssueUrl,
        String githubError
) {

    public static CreateTaskResponse from(RegisterIssueDraftsResponse.TaskResult result) {
        return new CreateTaskResponse(
                result.taskId(),
                result.title(),
                result.githubIssueRequested(),
                result.githubSyncStatus(),
                result.githubRepositoryId(),
                result.githubIssueNumber(),
                result.githubIssueUrl(),
                result.githubError()
        );
    }
}
