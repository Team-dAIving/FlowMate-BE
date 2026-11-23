package kr.co.daeun.flowmatebe.domain.meeting.controller;

import jakarta.validation.Valid;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CompleteRecordingUploadRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CreateMeetingRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CreateRecordingUploadRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.CreateMeetingResponse;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.MeetingResponse;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.RecordingUploadResponse;
import kr.co.daeun.flowmatebe.domain.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<CreateMeetingResponse> createMeeting(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        CreateMeetingResponse response = meetingService.create(userId(jwt), request);
        return ResponseEntity
                .created(URI.create("/api/meetings/" + response.meetingId()))
                .body(response);
    }

    @PostMapping("/{meetingId}/recording-upload")
    public ResponseEntity<RecordingUploadResponse> createRecordingUpload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @Valid @RequestBody CreateRecordingUploadRequest request
    ) {
        return ResponseEntity.ok(
                meetingService.createRecordingUpload(userId(jwt), meetingId, request)
        );
    }

    @PostMapping("/{meetingId}/recording-upload/complete")
    public ResponseEntity<MeetingResponse> completeRecordingUpload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @Valid @RequestBody CompleteRecordingUploadRequest request
    ) {
        return ResponseEntity.accepted().body(
                meetingService.completeRecordingUpload(userId(jwt), meetingId, request)
        );
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> getMeeting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(meetingService.get(userId(jwt), meetingId));
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
