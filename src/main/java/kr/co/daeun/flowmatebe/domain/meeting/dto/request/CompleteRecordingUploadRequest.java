package kr.co.daeun.flowmatebe.domain.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteRecordingUploadRequest(
        @NotNull(message = "녹음 길이는 필수입니다.")
        @Positive(message = "녹음 길이는 0초보다 커야 합니다.")
        Long durationSeconds
) {
}
