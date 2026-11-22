package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CreateProjectResponse(
        Long id,
        String name,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        Long teamId,
        int importedGithubIssueCount,
        String githubIssueImportStatus,
        List<ConnectedRepository> repositories
) {

    public static CreateProjectResponse of(
            Project project,
            List<ProjectRepository> repositories,
            Map<Long, String> rolesByRepositoryId,
            int importedGithubIssueCount,
            String githubIssueImportStatus
    ) {
        return new CreateProjectResponse(
                project.getId(),
                project.getName(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getTeam().getId(),
                importedGithubIssueCount,
                githubIssueImportStatus,
                repositories.stream()
                        .map(repository -> ConnectedRepository.from(
                                repository,
                                rolesByRepositoryId.get(repository.getGithubRepositoryId())
                        ))
                        .toList()
        );
    }

    public record ConnectedRepository(
            Long githubRepositoryId,
            Long installationId,
            String owner,
            String name,
            String htmlUrl,
            String role
    ) {

        private static ConnectedRepository from(
                ProjectRepository repository,
                String role
        ) {
            return new ConnectedRepository(
                    repository.getGithubRepositoryId(),
                    repository.getGithubInstallationId(),
                    repository.getOwnerLogin(),
                    repository.getRepositoryName(),
                    repository.getHtmlUrl(),
                    role
            );
        }
    }
}
