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

public record CreateProjectRequest(
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

        @NotNull(message = "팀 ID는 필수입니다.")
        @Positive(message = "팀 ID는 양수여야 합니다.")
        Long teamId,

        @NotNull(message = "프로젝트 리더는 필수입니다.")
        @Positive(message = "프로젝트 리더 ID는 양수여야 합니다.")
        Long leaderUserId,

        @NotEmpty(message = "프로젝트 멤버는 한 명 이상이어야 합니다.")
        @Size(max = 100, message = "프로젝트 멤버는 최대 100명까지 지정할 수 있습니다.")
        List<@Valid @NotNull(message = "프로젝트 멤버는 null일 수 없습니다.") Member> members,

        @NotNull(message = "추가 기술 스택 목록은 필수입니다.")
        @Size(max = 100, message = "추가 기술 스택은 최대 100개까지 지정할 수 있습니다.")
        List<@NotBlank(message = "기술 스택은 비어 있을 수 없습니다.")
                @Size(max = 100, message = "기술 스택은 100자 이하여야 합니다.") String> additionalTechStack,

        @NotNull(message = "GitHub 저장소 목록은 필수입니다.")
        @Size(max = 20, message = "GitHub 저장소는 최대 20개까지 연결할 수 있습니다.")
        List<@Valid @NotNull(message = "GitHub 저장소는 null일 수 없습니다.") Repository> repositories
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

            @NotEmpty(message = "프로젝트 멤버의 기술 스택은 한 개 이상이어야 합니다.")
            @Size(max = 100, message = "멤버별 기술 스택은 최대 100개까지 지정할 수 있습니다.")
            List<@NotBlank(message = "기술 스택은 비어 있을 수 없습니다.")
                    @Size(max = 100, message = "기술 스택은 100자 이하여야 합니다.") String> techStack
    ) {
    }

    public record Repository(
            @NotNull(message = "GitHub installation ID는 필수입니다.")
            @Positive(message = "GitHub installation ID는 양수여야 합니다.")
            Long installationId,

            @NotNull(message = "GitHub repository ID는 필수입니다.")
            @Positive(message = "GitHub repository ID는 양수여야 합니다.")
            Long githubRepositoryId,

            @NotBlank(message = "저장소 역할은 필수입니다.")
            @Pattern(
                    regexp = "^(common|backend|frontend|ai)$",
                    message = "저장소 역할은 common, backend, frontend, ai 중 하나여야 합니다."
            )
            String role
    ) {
    }
}
