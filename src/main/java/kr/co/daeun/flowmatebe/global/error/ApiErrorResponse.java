package kr.co.daeun.flowmatebe.global.error;

public record ApiErrorResponse(
        String code,
        String message
) {
}
