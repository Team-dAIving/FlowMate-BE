package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;

public record TeamMemberResponse(
        Long userId,
        String nickname,
        String avatarUrl
) {

    public static TeamMemberResponse from(TeamMember teamMember) {
        return new TeamMemberResponse(
                teamMember.getUser().getId(),
                teamMember.getUser().getNickname(),
                teamMember.getUser().getAvatarUrl()
        );
    }
}
