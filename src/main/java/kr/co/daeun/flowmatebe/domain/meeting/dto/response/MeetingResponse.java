package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;

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
        Transcript transcript
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
        return new MeetingResponse(
                meeting.getId(),
                meeting.getProject().getId(),
                meeting.getTitle(),
                meeting.getHeldAt(),
                meeting.getDurationSeconds(),
                meeting.getProcessingStatus(),
                meeting.getFailureReason(),
                meeting.getTranscriptionJobId(),
                transcript
        );
    }

    public record Transcript(
            String language,
            String fullText,
            List<Map<String, Object>> segments
    ) {
    }
}
