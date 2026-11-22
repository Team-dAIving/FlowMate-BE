package kr.co.daeun.flowmatebe.domain.task.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;

public record TaskGithubResponse(
        Long githubRepositoryId,
        String ownerLogin,
        String repositoryName,
        String repositoryUrl,
        Integer issueNumber,
        String issueUrl,
        String syncStatus
) {

    public static TaskGithubResponse from(Task task) {
        ProjectRepository repository = task.getGithubRepository();
        if (repository == null && task.getGithubIssueNumber() == null) {
            return null;
        }

        String repositoryUrl = repository == null ? null : repository.getHtmlUrl();
        Integer issueNumber = task.getGithubIssueNumber();
        String issueUrl = repositoryUrl == null || issueNumber == null
                ? null
                : repositoryUrl + "/issues/" + issueNumber;
        return new TaskGithubResponse(
                repository == null ? null : repository.getGithubRepositoryId(),
                repository == null ? null : repository.getOwnerLogin(),
                repository == null ? null : repository.getRepositoryName(),
                repositoryUrl,
                issueNumber,
                issueUrl,
                task.getGithubSyncStatus()
        );
    }
}
