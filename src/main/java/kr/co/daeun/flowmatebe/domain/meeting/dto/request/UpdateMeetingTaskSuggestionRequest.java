package kr.co.daeun.flowmatebe.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMeetingTaskSuggestionRequest(
        @NotBlank(message = "제안 처리 유형은 필수입니다.")
        @Pattern(
                regexp = "^(CREATE|UPDATE)$",
                message = "제안 처리 유형은 CREATE 또는 UPDATE여야 합니다."
        )
        String actionType,

        @Positive(message = "대상 업무 ID는 양수여야 합니다.")
        Long targetTaskId,

        @NotBlank(message = "업무 제목은 필수입니다.")
        @Size(max = 256, message = "업무 제목은 256자 이하여야 합니다.")
        String title,

        @NotNull(message = "업무 설명은 필수입니다.")
        @Size(max = 65536, message = "업무 설명은 65536자 이하여야 합니다.")
        String description,

        @Positive(message = "담당자 ID는 양수여야 합니다.")
        Long assigneeId,

        @NotNull(message = "업무 마감일은 필수입니다.")
        LocalDate dueDate,

        @NotBlank(message = "업무 우선순위는 필수입니다.")
        @Pattern(
                regexp = "^(LOW|MEDIUM|HIGH|URGENT)$",
                message = "업무 우선순위는 LOW, MEDIUM, HIGH, URGENT 중 하나여야 합니다."
        )
        String priority
) {
}
