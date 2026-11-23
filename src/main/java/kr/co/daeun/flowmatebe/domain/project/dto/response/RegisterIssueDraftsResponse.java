package kr.co.daeun.flowmatebe.domain.project.dto.response;

import java.util.List;

public record RegisterIssueDraftsResponse(
        int createdTaskCount,
        int createdGithubIssueCount,
        int failedGithubIssueCount,
        List<TaskResult> tasks
) {

    public record TaskResult(
            Long taskId,
            String title,
            boolean githubIssueRequested,
            String githubSyncStatus,
            Long githubRepositoryId,
            Integer githubIssueNumber,
            String githubIssueUrl,
            String githubError
    ) {
    }
}
