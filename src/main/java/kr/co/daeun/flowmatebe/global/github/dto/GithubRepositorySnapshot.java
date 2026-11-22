package kr.co.daeun.flowmatebe.global.github.dto;

import java.util.List;

public record GithubRepositorySnapshot(
        GithubRepositoryMetadata repository,
        List<GithubIssueMetadata> issues,
        boolean issueImportSucceeded
) {
}
