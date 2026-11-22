package kr.co.daeun.flowmatebe.domain.project.dto.response;

import java.util.List;

public record ProjectTasksResponse(
        long totalCount,
        List<ProjectTaskResponse> tasks
) {

    public ProjectTasksResponse(List<ProjectTaskResponse> tasks) {
        this(tasks.size(), tasks);
    }
}
