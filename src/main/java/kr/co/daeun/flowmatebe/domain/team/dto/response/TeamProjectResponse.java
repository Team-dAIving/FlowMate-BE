package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;

public record TeamProjectResponse(
        Long id,
        String name,
        String description,
        String goal
) {

    public static TeamProjectResponse from(Project project) {
        return new TeamProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getSubject()
        );
    }
}
