package kr.co.daeun.flowmatebe.global.github.dto;

import java.time.Instant;

public record GithubIssueMetadata(
        Integer number,
        String title,
        String body,
        String state,
        String assigneeLogin,
        Instant closedAt
) {
}
