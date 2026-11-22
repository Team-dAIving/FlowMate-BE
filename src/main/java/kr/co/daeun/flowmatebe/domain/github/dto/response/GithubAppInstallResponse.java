package kr.co.daeun.flowmatebe.domain.github.dto.response;

import java.time.Instant;

public record GithubAppInstallResponse(
        String installUrl,
        Instant expiresAt
) {
}
