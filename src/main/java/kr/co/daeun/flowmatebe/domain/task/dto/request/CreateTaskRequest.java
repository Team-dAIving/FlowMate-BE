package kr.co.daeun.flowmatebe.domain.task.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateTaskRequest(
        @NotBlank(message = "업무 제목은 필수입니다.")
        @Size(max = 256, message = "업무 제목은 256자 이하여야 합니다.")
        String title,

        @NotNull(message = "업무 설명은 필수입니다.")
        @Size(max = 65536, message = "업무 설명은 65536자 이하여야 합니다.")
        String description,

        @NotBlank(message = "업무 우선순위는 필수입니다.")
        @Pattern(
                regexp = "^(LOW|MEDIUM|HIGH|URGENT)$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "업무 우선순위는 LOW, MEDIUM, HIGH, URGENT 중 하나여야 합니다."
        )
        String priority,

        @Positive(message = "담당자 ID는 양수여야 합니다.")
        Long assigneeId,

        @NotNull(message = "업무 마감일은 필수입니다.")
        LocalDate dueDate,

        @DecimalMin(value = "0.01", message = "예상 시간은 0보다 커야 합니다.")
        @DecimalMax(value = "35791394", message = "예상 시간이 너무 큽니다.")
        Double estimatedHours,

        @Size(max = 20, message = "GitHub 라벨은 최대 20개까지 지정할 수 있습니다.")
        List<@NotBlank(message = "GitHub 라벨은 비어 있을 수 없습니다.")
                @Size(max = 50, message = "GitHub 라벨은 50자 이하여야 합니다.") String> labels,

        @NotNull(message = "GitHub 이슈 생성 여부는 필수입니다.")
        Boolean createGithubIssue,

        @Positive(message = "GitHub 저장소 ID는 양수여야 합니다.")
        Long githubRepositoryId
) {

    public CreateTaskRequest {
        labels = labels == null ? List.of() : labels;
    }
}
