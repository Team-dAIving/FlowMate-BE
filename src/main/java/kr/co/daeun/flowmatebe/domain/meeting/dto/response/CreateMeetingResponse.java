package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;

import java.time.Instant;

public record CreateMeetingResponse(
        Long meetingId,
        Long projectId,
        String title,
        Instant heldAt,
        String processingStatus
) {

    public static CreateMeetingResponse from(Meeting meeting) {
        return new CreateMeetingResponse(
                meeting.getId(),
                meeting.getProject().getId(),
                meeting.getTitle(),
                meeting.getHeldAt(),
                meeting.getProcessingStatus()
        );
    }
}
