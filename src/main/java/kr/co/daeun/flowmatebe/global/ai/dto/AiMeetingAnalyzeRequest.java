package kr.co.daeun.flowmatebe.global.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record AiMeetingAnalyzeRequest(
        Long meetingId,
        Long projectId,
        LocalDate meetingDate,
        Transcript transcript,
        List<Member> members,
        List<ExistingTask> existingTasks
) {

    public record Transcript(
            Long meetingId,
            String language,
            String fullText,
            List<Segment> segments
    ) {
    }

    public record Segment(
            String segmentId,
            String speakerId,
            Long startMs,
            Long endMs,
            String text
    ) {
    }

    public record Member(
            Long userId,
            String name,
            String role
    ) {
    }

    public record ExistingTask(
            Long taskId,
            String title,
            String description,
            Long assigneeId,
            LocalDate startDate,
            LocalDate dueDate,
            String priority,
            String status
    ) {
    }
}
