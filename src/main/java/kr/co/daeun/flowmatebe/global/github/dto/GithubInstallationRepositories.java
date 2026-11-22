package kr.co.daeun.flowmatebe.global.github.dto;

import java.util.List;

public record GithubInstallationRepositories(
        Long installationId,
        List<Repository> repositories
) {

    public record Repository(
            Long id,
            String owner,
            String name,
            String fullName,
            String htmlUrl,
            boolean privateRepository
    ) {
    }
}
