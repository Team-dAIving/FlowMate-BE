package kr.co.daeun.flowmatebe.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMeetingRequest(
        @NotNull(message = "프로젝트 ID는 필수입니다.")
        @Positive(message = "프로젝트 ID는 0보다 커야 합니다.")
        Long projectId,

        @NotBlank(message = "회의 제목은 필수입니다.")
        @Size(max = 200, message = "회의 제목은 200자 이하여야 합니다.")
        String title
) {
}
