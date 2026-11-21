package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.team.entity.Team;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String description,
        String themeColor,
        long memberCount,
        List<TeamProjectResponse> projects,
        List<TeamMemberResponse> members,
        long inProgressTaskCount
) {

    public static TeamResponse of(
            Team team,
            List<TeamProjectResponse> projects,
            List<TeamMemberResponse> members,
            long inProgressTaskCount
    ) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getThemeColor(),
                members.size(),
                List.copyOf(projects),
                List.copyOf(members),
                inProgressTaskCount
        );
    }
}
