package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;

public record ProjectMemberSummaryResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        String role
) {

    public static ProjectMemberSummaryResponse from(ProjectMember projectMember) {
        return new ProjectMemberSummaryResponse(
                projectMember.getUser().getId(),
                projectMember.getUser().getNickname(),
                projectMember.getUser().getAvatarUrl(),
                projectMember.getRole()
        );
    }
}
