package kr.co.daeun.flowmatebe.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ApproveMeetingTaskSuggestionRequest(
        @NotNull(message = "GitHub 이슈 생성 여부는 필수입니다.")
        Boolean createGithubIssue,

        @Positive(message = "GitHub 저장소 ID는 양수여야 합니다.")
        Long githubRepositoryId,

        @Size(max = 20, message = "GitHub 라벨은 최대 20개까지 지정할 수 있습니다.")
        List<@NotBlank(message = "GitHub 라벨은 비어 있을 수 없습니다.")
                @Size(max = 50, message = "GitHub 라벨은 50자 이하여야 합니다.") String> labels
) {

    public ApproveMeetingTaskSuggestionRequest {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
