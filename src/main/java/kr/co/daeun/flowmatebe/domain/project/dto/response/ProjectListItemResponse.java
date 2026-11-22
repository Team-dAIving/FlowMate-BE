package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;

import java.util.List;

public record ProjectListItemResponse(
        Long id,
        String type,
        String name,
        String description,
        String goal,
        String status,
        Long teamId,
        String teamName,
        String teamThemeColor,
        String leaderName,
        List<String> techStack,
        List<ProjectMemberSummaryResponse> members,
        long totalTaskCount,
        long completedTaskCount,
        int progressPercent
) {

    public static ProjectListItemResponse of(
            Project project,
            List<String> techStack,
            List<ProjectMemberSummaryResponse> members,
            String leaderName,
            long totalTaskCount,
            long completedTaskCount
    ) {
        int progressPercent = totalTaskCount == 0
                ? 0
                : (int) Math.round(completedTaskCount * 100.0 / totalTaskCount);
        return new ProjectListItemResponse(
                project.getId(),
                project.getType(),
                project.getName(),
                project.getDescription(),
                project.getSubject(),
                project.getStatus(),
                project.getTeam().getId(),
                project.getTeam().getName(),
                project.getTeam().getThemeColor(),
                leaderName,
                techStack,
                members,
                totalTaskCount,
                completedTaskCount,
                progressPercent
        );
    }
}
