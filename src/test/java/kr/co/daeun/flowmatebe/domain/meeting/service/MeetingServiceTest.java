package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CompleteRecordingUploadRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CreateMeetingRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.CreateRecordingUploadRequest;
import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingProcessingStatus;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.global.ai.client.AiTranscriptionClient;
import kr.co.daeun.flowmatebe.global.ai.dto.AiTranscriptionResponse;
import kr.co.daeun.flowmatebe.global.storage.service.MeetingRecordingStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingServiceTest {

    private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final MeetingRecordingStorage recordingStorage = mock(MeetingRecordingStorage.class);
    private final AiTranscriptionClient transcriptionClient = mock(AiTranscriptionClient.class);
    private final MeetingService service = new MeetingService(
            meetingRepository,
            projectRepository,
            projectMemberRepository,
            recordingStorage,
            transcriptionClient
    );

    @TempDir
    Path tempDir;

    @Test
    void 접근할_수_있는_프로젝트에_회의를_생성한다() {
        Project project = project(10L);
        when(projectRepository.findAccessibleByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(project));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting meeting = invocation.getArgument(0);
            meeting.setId(20L);
            return meeting;
        });

        var response = service.create(1L, new CreateMeetingRequest(10L, "  주간 회의  "));

        assertThat(response.meetingId()).isEqualTo(20L);
        assertThat(response.title()).isEqualTo("주간 회의");
        assertThat(response.processingStatus()).isEqualTo("RECORDING");
    }

    @Test
    void 녹음_업로드_URL을_발급하고_S3_key를_저장한다() {
        Meeting meeting = meeting(20L, MeetingProcessingStatus.RECORDING);
        when(meetingRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(meeting));
        when(recordingStorage.createUploadTarget(20L, "audio/webm"))
                .thenReturn(new MeetingRecordingStorage.UploadTarget(
                        "flowmate/meetings/20/recording.webm",
                        "https://s3.test/upload",
                        Map.of("content-type", "audio/webm"),
                        Instant.parse("2026-11-24T01:00:00Z")
                ));

        var response = service.createRecordingUpload(
                1L,
                20L,
                new CreateRecordingUploadRequest("audio/webm")
        );

        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.uploadUrl()).isEqualTo("https://s3.test/upload");
        assertThat(meeting.getRecordingS3Key())
                .isEqualTo("flowmate/meetings/20/recording.webm");
        assertThat(meeting.getProcessingStatus()).isEqualTo("UPLOAD_PENDING");
    }

    @Test
    void S3_녹음본을_AI에_전달하고_jobId를_저장한다() throws Exception {
        Meeting meeting = meeting(20L, MeetingProcessingStatus.UPLOAD_PENDING);
        meeting.setRecordingS3Key("flowmate/meetings/20/recording.webm");
        Path file = Files.writeString(tempDir.resolve("recording.webm"), "recording");
        MeetingRecordingStorage.DownloadedRecording recording =
                new MeetingRecordingStorage.DownloadedRecording(
                        tempDir,
                        file,
                        "recording.webm",
                        "audio/webm"
                );
        when(meetingRepository.findAccessibleByIdAndUserId(20L, 1L))
                .thenReturn(Optional.of(meeting));
        when(recordingStorage.download("flowmate/meetings/20/recording.webm"))
                .thenReturn(recording);
        when(transcriptionClient.upload(
                20L,
                file,
                "recording.webm",
                "audio/webm"
        )).thenReturn(new AiTranscriptionResponse(
                20L,
                "job-abc",
                "QUEUED",
                null,
                null
        ));

        var response = service.completeRecordingUpload(
                1L,
                20L,
                new CompleteRecordingUploadRequest(3600L)
        );

        assertThat(response.transcriptionJobId()).isEqualTo("job-abc");
        assertThat(response.processingStatus()).isEqualTo("STT_QUEUED");
        assertThat(meeting.getDurationSeconds()).isEqualTo(3600L);
        assertThat(file).doesNotExist();
    }

    @Test
    void STT가_완료되면_전체_텍스트와_segment를_저장한다() {
        Meeting meeting = meeting(20L, MeetingProcessingStatus.STT_IN_PROGRESS);
        meeting.setTranscriptionJobId("job-abc");
        when(meetingRepository.findById(20L)).thenReturn(Optional.of(meeting));
        when(transcriptionClient.get("job-abc", 20L)).thenReturn(
                new AiTranscriptionResponse(
                        20L,
                        "job-abc",
                        "COMPLETED",
                        new AiTranscriptionResponse.Transcript(
                                20L,
                                "ko-KR",
                                "회의 전체 내용",
                                List.of(new AiTranscriptionResponse.Segment(
                                        "segment-1",
                                        "speaker-1",
                                        0L,
                                        1500L,
                                        "첫 번째 발언"
                                ))
                        ),
                        null
                )
        );

        service.refreshTranscription(20L);

        assertThat(meeting.getProcessingStatus()).isEqualTo("STT_COMPLETED");
        assertThat(meeting.getTranscriptLanguage()).isEqualTo("ko-KR");
        assertThat(meeting.getTranscript()).isEqualTo("회의 전체 내용");
        assertThat(meeting.getTranscriptSegments()).singleElement().satisfies(segment -> {
            assertThat(segment.get("segmentId")).isEqualTo("segment-1");
            assertThat(segment.get("startMs")).isEqualTo(0L);
            assertThat(segment.get("text")).isEqualTo("첫 번째 발언");
        });
        verify(transcriptionClient).get("job-abc", 20L);
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        return project;
    }

    private Meeting meeting(Long id, MeetingProcessingStatus status) {
        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setProject(project(10L));
        meeting.setTitle("주간 회의");
        meeting.setProcessingStatus(status.name());
        return meeting;
    }
}
