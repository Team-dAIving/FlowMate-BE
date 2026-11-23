package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import java.time.Instant;
import java.util.Map;

public record RecordingUploadResponse(
        Long meetingId,
        String objectKey,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt
) {
}
