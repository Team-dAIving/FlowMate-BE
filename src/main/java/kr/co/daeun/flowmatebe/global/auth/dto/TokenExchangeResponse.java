package kr.co.daeun.flowmatebe.global.auth.dto;

public record TokenExchangeResponse(
        String accessToken,
        boolean onboardingRequired
) {
}
