package kr.co.daeun.flowmatebe.domain.project.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ProjectDetailResponse(
        Long id,
        String type,
        String name,
        String description,
        String goal,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        Leader leader,
        Team team,
        CommonRepository commonRepository,
        Counts counts,
        Progress progress,
        Map<String, Long> taskStatusCounts,
        List<Workload> workload,
        List<Velocity> velocity,
        List<ProjectTaskResponse> recentTasks
) {

    public record Leader(
            Long userId,
            String nickname,
            String avatarUrl
    ) {
    }

    public record Team(
            Long id,
            String name,
            String themeColor
    ) {
    }

    public record CommonRepository(
            Long id,
            Long githubRepositoryId,
            String ownerLogin,
            String repositoryName,
            String htmlUrl
    ) {
    }

    public record Counts(
            long taskCount,
            long meetingCount,
            long memberCount
    ) {
    }

    public record Progress(
            long totalTaskCount,
            long completedTaskCount,
            int percent
    ) {
    }

    public record Workload(
            Long projectMemberId,
            Long userId,
            String nickname,
            String avatarUrl,
            long activeTaskCount,
            long completedTaskCount
    ) {
    }

    public record Velocity(
            LocalDate date,
            long completedTaskCount
    ) {
    }
}
