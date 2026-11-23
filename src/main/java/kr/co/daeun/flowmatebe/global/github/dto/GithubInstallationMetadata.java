package kr.co.daeun.flowmatebe.global.github.dto;

public record GithubInstallationMetadata(
        Long installationId,
        Long accountId,
        String accountLogin,
        String accountType
) {
}
