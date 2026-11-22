package kr.co.daeun.flowmatebe.global.github.dto;

public record GithubRepositoryMetadata(
        Long githubRepositoryId,
        Long installationId,
        String ownerLogin,
        String repositoryName,
        String htmlUrl
) {
}
