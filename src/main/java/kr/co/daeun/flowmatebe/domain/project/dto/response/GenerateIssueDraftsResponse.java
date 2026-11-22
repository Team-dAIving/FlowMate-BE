package kr.co.daeun.flowmatebe.domain.project.dto.response;

import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateResponse;

import java.time.LocalDate;
import java.util.List;

public record GenerateIssueDraftsResponse(
        List<IssueDraft> issues
) {

    public static GenerateIssueDraftsResponse from(AiIssueGenerateResponse response) {
        return new GenerateIssueDraftsResponse(
                response.issues().stream()
                        .map(IssueDraft::from)
                        .toList()
        );
    }

    public record IssueDraft(
            String title,
            String description,
            String priority,
            Long assigneeId,
            LocalDate startDate,
            LocalDate dueDate,
            Double estimatedHours,
            List<String> labels
    ) {

        private static IssueDraft from(AiIssueGenerateResponse.Issue issue) {
            return new IssueDraft(
                    issue.title(),
                    issue.description(),
                    issue.priority(),
                    issue.assigneeId(),
                    issue.startDate(),
                    issue.dueDate(),
                    issue.estimatedHours(),
                    issue.labels() == null ? List.of() : List.copyOf(issue.labels())
            );
        }
    }
}
