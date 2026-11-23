package kr.co.daeun.flowmatebe.global.ai.dto;

import java.util.List;

public record AiTranscriptionResponse(
        Long meetingId,
        String jobId,
        String status,
        Transcript transcript,
        String failureReason
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
}
