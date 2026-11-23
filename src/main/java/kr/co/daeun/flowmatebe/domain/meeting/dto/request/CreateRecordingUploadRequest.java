package kr.co.daeun.flowmatebe.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecordingUploadRequest(
        @NotBlank(message = "녹음 파일 Content-Type은 필수입니다.")
        @Size(max = 100, message = "Content-Type은 100자 이하여야 합니다.")
        String contentType
) {
}
