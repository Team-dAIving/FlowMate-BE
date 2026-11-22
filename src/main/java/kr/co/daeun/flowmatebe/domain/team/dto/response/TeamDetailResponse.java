package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.team.entity.Team;

import java.util.List;

public record TeamDetailResponse(
        Long id,
        String name,
        String description,
        String themeColor,
        long projectCount,
        long memberCount,
        long taskCount,
        long inProgressTaskCount,
        long completedTaskCount,
        List<TeamDetailProjectResponse> projects,
        List<TeamDetailMemberResponse> members,
        List<TeamTaskResponse> tasks
) {

    public TeamDetailResponse {
        projects = List.copyOf(projects);
        members = List.copyOf(members);
        tasks = List.copyOf(tasks);
    }

    public static TeamDetailResponse of(
            Team team,
            List<TeamDetailProjectResponse> projects,
            List<TeamDetailMemberResponse> members,
            List<TeamTaskResponse> tasks,
            long inProgressTaskCount,
            long completedTaskCount
    ) {
        return new TeamDetailResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getThemeColor(),
                projects.size(),
                members.size(),
                tasks.size(),
                inProgressTaskCount,
                completedTaskCount,
                projects,
                members,
                tasks
        );
    }
}
