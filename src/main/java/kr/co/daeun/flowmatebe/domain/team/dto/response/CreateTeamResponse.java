package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.team.entity.Team;

import java.util.List;

public record CreateTeamResponse(
        Long id,
        String name,
        String description,
        String themeColor,
        long memberCount,
        List<TeamMemberResponse> members
) {

    public CreateTeamResponse {
        members = List.copyOf(members);
    }

    public static CreateTeamResponse of(
            Team team,
            List<TeamMemberResponse> members
    ) {
        return new CreateTeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getThemeColor(),
                members.size(),
                members
        );
    }
}
