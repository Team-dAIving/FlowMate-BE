package kr.co.daeun.flowmatebe.domain.task.dto.response;

import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectTaskResponse;

import java.time.LocalDate;
import java.util.List;

public record MyProjectTasksResponse(
        long totalProjectCount,
        long totalTaskCount,
        List<ProjectTasks> projects
) {

    public MyProjectTasksResponse(List<ProjectTasks> projects) {
        this(
                projects.size(),
                projects.stream().mapToLong(ProjectTasks::taskCount).sum(),
                projects
        );
    }

    public record ProjectTasks(
            Long projectId,
            String projectName,
            String projectStatus,
            LocalDate projectStartDate,
            LocalDate projectEndDate,
            long taskCount,
            List<ProjectTaskResponse> tasks
    ) {

        public ProjectTasks(
                Long projectId,
                String projectName,
                String projectStatus,
                LocalDate projectStartDate,
                LocalDate projectEndDate,
                List<ProjectTaskResponse> tasks
        ) {
            this(
                    projectId,
                    projectName,
                    projectStatus,
                    projectStartDate,
                    projectEndDate,
                    tasks.size(),
                    tasks
            );
        }
    }
}
