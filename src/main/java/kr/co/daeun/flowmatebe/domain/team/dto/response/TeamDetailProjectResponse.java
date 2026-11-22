package kr.co.daeun.flowmatebe.domain.team.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;

public record TeamDetailProjectResponse(
        Long id,
        String name,
        String description,
        String subject,
        long totalTaskCount,
        long completedTaskCount,
        int progressPercent
) {

    public static TeamDetailProjectResponse of(
            Project project,
            long totalTaskCount,
            long completedTaskCount
    ) {
        int progressPercent = totalTaskCount == 0
                ? 0
                : (int) Math.round(completedTaskCount * 100.0 / totalTaskCount);
        return new TeamDetailProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getSubject(),
                totalTaskCount,
                completedTaskCount,
                progressPercent
        );
    }
}
