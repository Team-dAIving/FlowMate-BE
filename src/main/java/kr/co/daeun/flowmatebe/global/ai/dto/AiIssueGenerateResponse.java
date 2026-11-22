package kr.co.daeun.flowmatebe.global.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record AiIssueGenerateResponse(
        List<Issue> issues
) {

    public record Issue(
            String title,
            String description,
            String priority,
            Long assigneeId,
            LocalDate startDate,
            LocalDate dueDate,
            Double estimatedHours,
            List<String> labels
    ) {
    }
}
