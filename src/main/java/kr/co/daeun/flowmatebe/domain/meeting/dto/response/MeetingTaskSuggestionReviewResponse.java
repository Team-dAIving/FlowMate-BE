package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import java.util.Map;

public record MeetingTaskSuggestionReviewResponse(
        Long suggestionId,
        String reviewStatus,
        String actionType,
        Long taskId,
        Map<String, Object> proposedData,
        Boolean githubIssueRequested,
        String githubSyncStatus,
        Long githubRepositoryId,
        Integer githubIssueNumber,
        String githubIssueUrl,
        String githubError
) {
}
