package kr.co.daeun.flowmatebe.domain.project.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record GenerateIssueDraftsRequest(
        @NotBlank(message = "프로젝트명은 필수입니다.")
        @Size(max = 200, message = "프로젝트명은 200자 이하여야 합니다.")
        String name,

        @NotBlank(message = "프로젝트 설명은 필수입니다.")
        @Size(max = 5000, message = "프로젝트 설명은 5000자 이하여야 합니다.")
        String description,

        @NotBlank(message = "프로젝트 최종 목표는 필수입니다.")
        @Size(max = 2000, message = "프로젝트 최종 목표는 2000자 이하여야 합니다.")
        String goal,

        @NotBlank(message = "프로젝트 설계 내용은 필수입니다.")
        @Size(max = 50000, message = "프로젝트 설계 내용은 50000자 이하여야 합니다.")
        String designText,

        @NotBlank(message = "프로젝트 유형은 필수입니다.")
        @Size(max = 50, message = "프로젝트 유형은 50자 이하여야 합니다.")
        String type,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료 예정일은 필수입니다.")
        LocalDate endDate,

        @Positive(message = "팀 ID는 양수여야 합니다.")
        Long teamId,

        @NotNull(message = "프로젝트 리더는 필수입니다.")
        @Positive(message = "프로젝트 리더 ID는 양수여야 합니다.")
        Long leaderUserId,

        @NotEmpty(message = "프로젝트 멤버는 한 명 이상이어야 합니다.")
        @Size(max = 100, message = "프로젝트 멤버는 최대 100명까지 지정할 수 있습니다.")
        List<@Valid @NotNull(message = "프로젝트 멤버는 null일 수 없습니다.") Member> members
) {

    public record Member(
            @NotNull(message = "프로젝트 멤버의 사용자 ID는 필수입니다.")
            @Positive(message = "사용자 ID는 양수여야 합니다.")
            Long userId,

            @NotBlank(message = "프로젝트 멤버의 파트는 필수입니다.")
            @Pattern(
                    regexp = "^(backend|fullstack|frontend|pm|design|others|infra)$",
                    message = "파트는 backend, fullstack, frontend, pm, design, others, infra 중 하나여야 합니다."
            )
            String part,

            @NotBlank(message = "프로젝트 멤버의 기술 스택은 필수입니다.")
            @Size(max = 1000, message = "멤버별 기술 스택은 1000자 이하여야 합니다.")
            String techStack,

            @Size(max = 500, message = "GitHub 저장소 주소는 500자 이하여야 합니다.")
            String githubRepositoryUrl
    ) {
    }
}
