package kr.co.daeun.flowmatebe.global.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record AiIssueGenerateRequest(
        Project project,
        String planText,
        List<Member> members
) {

    public record Project(
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            List<String> techStack
    ) {
    }

    public record Member(
            Long userId,
            String name,
            String role
    ) {
    }
}
