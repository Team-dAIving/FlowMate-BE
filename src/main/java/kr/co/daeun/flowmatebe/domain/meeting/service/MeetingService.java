package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CompleteRecordingUploadRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CreateMeetingRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CreateRecordingUploadRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.CreateMeetingResponse;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.MeetingResponse;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.RecordingUploadResponse;
import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingProcessingStatus;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.global.ai.client.AiTranscriptionClient;
import kr.co.daeun.flowmatebe.global.ai.dto.AiTranscriptionResponse;
import kr.co.daeun.flowmatebe.global.storage.service.MeetingRecordingStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final Set<String> POLLABLE_STATUSES = Set.of(
            MeetingProcessingStatus.STT_QUEUED.name(),
            MeetingProcessingStatus.STT_IN_PROGRESS.name()
    );

    private final MeetingRepository meetingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MeetingRecordingStorage recordingStorage;
    private final AiTranscriptionClient transcriptionClient;

    @Transactional
    public CreateMeetingResponse create(Long currentUserId, CreateMeetingRequest request) {
        Project project = projectRepository
                .findAccessibleByIdAndUserId(request.projectId(), currentUserId)
                .orElseThrow(() -> notFound("프로젝트를 찾을 수 없습니다."));
        Meeting meeting = new Meeting();
        meeting.setProject(project);
        meeting.setTitle(request.title().strip());
        meeting.setHeldAt(Instant.now());
        meeting.setProcessingStatus(MeetingProcessingStatus.RECORDING.name());
        return CreateMeetingResponse.from(meetingRepository.save(meeting));
    }

    @Transactional
    public RecordingUploadResponse createRecordingUpload(
            Long currentUserId,
            Long meetingId,
            CreateRecordingUploadRequest request
    ) {
        Meeting meeting = getAccessibleMeeting(currentUserId, meetingId);
        if (isSttStarted(meeting)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 음성 변환이 시작된 회의입니다."
            );
        }
        MeetingRecordingStorage.UploadTarget target = recordingStorage.createUploadTarget(
                meetingId,
                request.contentType()
        );
        meeting.setRecordingS3Key(target.objectKey());
        meeting.setProcessingStatus(MeetingProcessingStatus.UPLOAD_PENDING.name());
        meeting.setFailureReason(null);
        return new RecordingUploadResponse(
                meetingId,
                target.objectKey(),
                target.uploadUrl(),
                "PUT",
                target.headers(),
                target.expiresAt()
        );
    }

    @Transactional
    public MeetingResponse completeRecordingUpload(
            Long currentUserId,
            Long meetingId,
            CompleteRecordingUploadRequest request
    ) {
        Meeting meeting = getAccessibleMeeting(currentUserId, meetingId);
        if (isSttStarted(meeting)) {
            return MeetingResponse.from(meeting);
        }
        if (!MeetingProcessingStatus.UPLOAD_PENDING.name().equals(meeting.getProcessingStatus())
                || meeting.getRecordingS3Key() == null
                || meeting.getRecordingS3Key().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "먼저 녹음 업로드 URL을 발급받아 파일을 업로드해야 합니다."
            );
        }

        try (MeetingRecordingStorage.DownloadedRecording recording =
                     recordingStorage.download(meeting.getRecordingS3Key())) {
            AiTranscriptionResponse response = transcriptionClient.upload(
                    meetingId,
                    recording.file(),
                    recording.filename(),
                    recording.contentType()
            );
            meeting.setDurationSeconds(request.durationSeconds());
            applyTranscriptionResponse(meeting, response);
            return MeetingResponse.from(meeting);
        }
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(Long currentUserId, Long meetingId) {
        Meeting meeting = getAccessibleMeeting(currentUserId, meetingId);
        return MeetingResponse.from(
                meeting,
                projectMemberRepository.findAllWithUserByProjectId(
                        meeting.getProject().getId()
                )
        );
    }

    @Transactional
    public void refreshTranscription(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null
                || !POLLABLE_STATUSES.contains(meeting.getProcessingStatus())
                || meeting.getTranscriptionJobId() == null
                || meeting.getTranscriptionJobId().isBlank()) {
            return;
        }
        AiTranscriptionResponse response = transcriptionClient.get(
                meeting.getTranscriptionJobId(),
                meeting.getId()
        );
        applyTranscriptionResponse(meeting, response);
    }

    private void applyTranscriptionResponse(
            Meeting meeting,
            AiTranscriptionResponse response
    ) {
        meeting.setTranscriptionJobId(response.jobId());
        switch (response.status()) {
            case "QUEUED" -> meeting.setProcessingStatus(
                    MeetingProcessingStatus.STT_QUEUED.name()
            );
            case "IN_PROGRESS" -> meeting.setProcessingStatus(
                    MeetingProcessingStatus.STT_IN_PROGRESS.name()
            );
            case "FAILED" -> {
                meeting.setProcessingStatus(MeetingProcessingStatus.FAILED.name());
                meeting.setFailureReason(normalizeFailureReason(response.failureReason()));
            }
            case "COMPLETED" -> applyCompletedTranscript(meeting, response.transcript());
            default -> throw invalidAiResponse();
        }
    }

    private void applyCompletedTranscript(
            Meeting meeting,
            AiTranscriptionResponse.Transcript transcript
    ) {
        if (transcript == null
                || transcript.meetingId() == null
                || !transcript.meetingId().equals(meeting.getId())
                || transcript.language() == null
                || transcript.language().isBlank()
                || transcript.fullText() == null
                || transcript.segments() == null) {
            throw invalidAiResponse();
        }
        List<Map<String, Object>> segments = new ArrayList<>();
        for (AiTranscriptionResponse.Segment segment : transcript.segments()) {
            if (segment == null
                    || segment.segmentId() == null
                    || segment.segmentId().isBlank()
                    || segment.startMs() == null
                    || segment.startMs() < 0
                    || segment.endMs() == null
                    || segment.endMs() < segment.startMs()
                    || segment.text() == null) {
                throw invalidAiResponse();
            }
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("segmentId", segment.segmentId());
            value.put("speakerId", segment.speakerId());
            value.put("startMs", segment.startMs());
            value.put("endMs", segment.endMs());
            value.put("text", segment.text());
            segments.add(value);
        }
        meeting.setTranscriptLanguage(transcript.language());
        meeting.setTranscript(transcript.fullText());
        meeting.setTranscriptSegments(segments);
        meeting.setFailureReason(null);
        meeting.setProcessingStatus(MeetingProcessingStatus.STT_COMPLETED.name());
    }

    private boolean isSttStarted(Meeting meeting) {
        return meeting.getTranscriptionJobId() != null
                && !meeting.getTranscriptionJobId().isBlank();
    }

    private String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "AI 음성 변환 작업이 실패했습니다.";
        }
        return failureReason.strip();
    }

    private Meeting getAccessibleMeeting(Long currentUserId, Long meetingId) {
        return meetingRepository.findAccessibleByIdAndUserId(meetingId, currentUserId)
                .orElseThrow(() -> notFound("회의를 찾을 수 없습니다."));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException invalidAiResponse() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI STT 서버가 유효하지 않은 transcript를 반환했습니다."
        );
    }
}
