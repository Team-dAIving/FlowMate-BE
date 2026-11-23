package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MeetingResponse(
        Long meetingId,
        Long projectId,
        String title,
        Instant heldAt,
        Long durationSeconds,
        String processingStatus,
        String failureReason,
        String transcriptionJobId,
        Transcript transcript,
        Analysis analysis,
        List<TaskSuggestion> taskSuggestions
) {

    public static MeetingResponse from(Meeting meeting) {
        Transcript transcript = meeting.getTranscript() == null
                ? null
                : new Transcript(
                        meeting.getTranscriptLanguage(),
                        meeting.getTranscript(),
                        meeting.getTranscriptSegments() == null
                                ? List.of()
                                : List.copyOf(meeting.getTranscriptSegments())
                );
        Analysis analysis = meeting.getSummary() == null
                ? null
                : new Analysis(
                        meeting.getSummary(),
                        copyList(meeting.getTopics()),
                        copyMapList(meeting.getDecisions()),
                        copyList(meeting.getUnresolvedItems())
                );
        return new MeetingResponse(
                meeting.getId(),
                meeting.getProject().getId(),
                meeting.getTitle(),
                meeting.getHeldAt(),
                meeting.getDurationSeconds(),
                meeting.getProcessingStatus(),
                meeting.getFailureReason(),
                meeting.getTranscriptionJobId(),
                transcript,
                analysis,
                meeting.getTaskSuggestions() == null
                        ? List.of()
                        : meeting.getTaskSuggestions().stream()
                                .map(TaskSuggestion::from)
                                .toList()
        );
    }

    private static List<String> copyList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<Map<String, Object>> copyMapList(
            List<Map<String, Object>> values
    ) {
        return values == null
                ? List.of()
                : values.stream().map(Map::copyOf).toList();
    }

    public record Transcript(
            String language,
            String fullText,
            List<Map<String, Object>> segments
    ) {
    }

    public record Analysis(
            String summary,
            List<String> topics,
            List<Map<String, Object>> decisions,
            List<String> unresolvedItems
    ) {
    }

    public record TaskSuggestion(
            Long suggestionId,
            String suggestionType,
            Long targetTaskId,
            Map<String, Object> proposedData,
            String reviewStatus,
            Instant createdAt
    ) {

        private static TaskSuggestion from(MeetingTaskSuggestion suggestion) {
            return new TaskSuggestion(
                    suggestion.getId(),
                    suggestion.getSuggestionType(),
                    suggestion.getTargetTaskId(),
                    suggestion.getProposedData(),
                    suggestion.getReviewStatus(),
                    suggestion.getCreatedAt()
            );
        }
    }
}
