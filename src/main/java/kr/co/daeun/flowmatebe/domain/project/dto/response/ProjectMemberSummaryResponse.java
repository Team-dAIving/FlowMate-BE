package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;

import java.util.List;

public record ProjectMemberSummaryResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        String role,
        List<String> parts,
        List<String> techStack
) {

    public static ProjectMemberSummaryResponse from(ProjectMember projectMember) {
        return new ProjectMemberSummaryResponse(
                projectMember.getUser().getId(),
                projectMember.getUser().getNickname(),
                projectMember.getUser().getAvatarUrl(),
                projectMember.getRole(),
                copyOrEmpty(projectMember.getParts()),
                copyOrEmpty(projectMember.getTechStack())
        );
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
