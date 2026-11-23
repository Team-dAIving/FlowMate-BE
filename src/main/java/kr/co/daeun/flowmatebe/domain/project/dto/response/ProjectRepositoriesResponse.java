package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;

import java.util.List;

public record ProjectRepositoriesResponse(
        long totalCount,
        List<Repository> repositories
) {

    public ProjectRepositoriesResponse(List<Repository> repositories) {
        this(repositories.size(), repositories);
    }

    public record Repository(
            Long githubRepositoryId,
            String owner,
            String name,
            String htmlUrl,
            String role
    ) {

        public static Repository from(ProjectRepository repository) {
            return new Repository(
                    repository.getGithubRepositoryId(),
                    repository.getOwnerLogin(),
                    repository.getRepositoryName(),
                    repository.getHtmlUrl(),
                    repository.getRepositoryRole()
            );
        }
    }
}
