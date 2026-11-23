package kr.co.daeun.flowmatebe.domain.github.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteGithubInstallationRequest(
        @NotNull(message = "GitHub installation ID는 필수입니다.")
        @Positive(message = "GitHub installation ID는 양수여야 합니다.")
        Long installationId,

        @NotBlank(message = "GitHub App 설치 state는 필수입니다.")
        String state
) {
}
