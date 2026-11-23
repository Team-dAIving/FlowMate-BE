package kr.co.daeun.flowmatebe.global.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record AiMeetingAnalyzeResponse(
        Long meetingId,
        Long projectId,
        String summary,
        List<String> topics,
        List<Decision> decisions,
        List<String> unresolvedItems,
        List<ActionItem> actionItems
) {

    public record Decision(
            String content,
            List<String> evidenceSegmentIds
    ) {
    }

    public record ActionItem(
            String actionType,
            Long targetTaskId,
            String title,
            String description,
            Long assigneeId,
            LocalDate dueDate,
            TaskChanges changes,
            List<String> evidenceSegmentIds,
            Boolean requiresReview,
            String reason
    ) {
    }

    public record TaskChanges(
            String title,
            String description,
            Long assigneeId,
            LocalDate startDate,
            LocalDate dueDate,
            String priority
    ) {
    }
}
