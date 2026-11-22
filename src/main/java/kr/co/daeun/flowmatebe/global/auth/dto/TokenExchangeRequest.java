package kr.co.daeun.flowmatebe.global.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TokenExchangeRequest(
        @NotBlank(message = "code는 필수입니다.")
        String code,

        @NotBlank(message = "client는 필수입니다.")
        @Pattern(regexp = "desktop|web", message = "client는 desktop 또는 web이어야 합니다.")
        String client
) {
}
