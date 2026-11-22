package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;

public record TeamDetailMemberResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        long totalAssignedTaskCount,
        long inProgressAssignedTaskCount
) {

    public static TeamDetailMemberResponse of(
            TeamMember teamMember,
            long totalAssignedTaskCount,
            long inProgressAssignedTaskCount
    ) {
        return new TeamDetailMemberResponse(
                teamMember.getUser().getId(),
                teamMember.getUser().getNickname(),
                teamMember.getUser().getAvatarUrl(),
                totalAssignedTaskCount,
                inProgressAssignedTaskCount
        );
    }
}
