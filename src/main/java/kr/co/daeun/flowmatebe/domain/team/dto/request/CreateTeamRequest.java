package kr.co.daeun.flowmatebe.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTeamRequest(
        @NotBlank(message = "팀 이름은 필수입니다.")
        @Size(max = 100, message = "팀 이름은 100자 이하여야 합니다.")
        String name,

        @Size(max = 2000, message = "팀 설명은 2000자 이하여야 합니다.")
        String description,

        @NotBlank(message = "테마 색상은 필수입니다.")
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "테마 색상은 #FFFFFF 형식의 6자리 색상 코드여야 합니다."
        )
        String themeColor,

        @NotNull(message = "팀원 목록은 필수입니다.")
        @Size(max = 100, message = "한 팀에는 최대 100명까지 추가할 수 있습니다.")
        List<@NotNull(message = "사용자 ID는 null일 수 없습니다.") Long> memberUserIds
) {
}
