package kr.co.daeun.flowmatebe.domain.github.dto.response;

import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationRepositories;

import java.util.List;

public record GithubInstallationListResponse(
        List<Installation> installations
) {

    public record Installation(
            Long installationId,
            String accountLogin,
            String accountType,
            List<GithubInstallationRepositories.Repository> repositories
    ) {
    }
}
