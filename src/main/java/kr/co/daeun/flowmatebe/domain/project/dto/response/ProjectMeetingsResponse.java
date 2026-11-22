package kr.co.daeun.flowmatebe.domain.project.dto.response;

import java.time.Instant;
import java.util.List;

public record ProjectMeetingsResponse(
        long totalCount,
        List<Meeting> meetings
) {

    public ProjectMeetingsResponse(List<Meeting> meetings) {
        this(meetings.size(), meetings);
    }

    public record Meeting(
            Long id,
            String title,
            String summary,
            Instant heldAt,
            Long durationSeconds,
            String processingStatus,
            long approvedActionItemCount
    ) {
    }
}
