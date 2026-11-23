package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingProcessingStatus;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingTaskSuggestionRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.global.ai.client.AiMeetingAnalysisClient;
import kr.co.daeun.flowmatebe.global.ai.dto.AiMeetingAnalyzeRequest;
import kr.co.daeun.flowmatebe.global.ai.dto.AiMeetingAnalyzeResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingAnalysisServiceTest {

    private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final MeetingTaskSuggestionRepository suggestionRepository =
            mock(MeetingTaskSuggestionRepository.class);
    private final AiMeetingAnalysisClient analysisClient = mock(AiMeetingAnalysisClient.class);
    private final MeetingAnalysisService service = new MeetingAnalysisService(
            meetingRepository,
            projectMemberRepository,
            taskRepository,
            suggestionRepository,
            analysisClient
    );

    @Test
    void STT_완료_회의를_한국_날짜와_DB_데이터로_분석하고_제안을_저장한다() {
        Meeting meeting = meeting(MeetingProcessingStatus.STT_COMPLETED);
        ProjectMember member = member(7L, "다은", "BACKEND");
        Task task = task(30L, member);
        task.setPriority(null);
        when(meetingRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(meeting));
        when(projectMemberRepository.findAllWithUserByProjectId(10L))
                .thenReturn(List.of(member));
        when(taskRepository.findAllWithDetailsByProjectId(10L)).thenReturn(List.of(task));
        when(analysisClient.analyze(any())).thenReturn(successResponse());

        service.analyze(20L);

        ArgumentCaptor<AiMeetingAnalyzeRequest> requestCaptor =
                ArgumentCaptor.forClass(AiMeetingAnalyzeRequest.class);
        verify(analysisClient).analyze(requestCaptor.capture());
        AiMeetingAnalyzeRequest request = requestCaptor.getValue();
        assertThat(request.meetingId()).isEqualTo(20L);
        assertThat(request.projectId()).isEqualTo(10L);
        assertThat(request.meetingDate()).isEqualTo(LocalDate.of(2026, 11, 24));
        assertThat(request.transcript().fullText()).isEqualTo("회의 전체 내용");
        assertThat(request.transcript().segments()).singleElement().satisfies(segment -> {
            assertThat(segment.segmentId()).isEqualTo("segment-1");
            assertThat(segment.startMs()).isZero();
        });
        assertThat(request.members()).singleElement().satisfies(value -> {
            assertThat(value.userId()).isEqualTo(7L);
            assertThat(value.name()).isEqualTo("다은");
            assertThat(value.role()).isEqualTo("BACKEND");
        });
        assertThat(request.existingTasks()).singleElement().satisfies(value -> {
            assertThat(value.taskId()).isEqualTo(30L);
            assertThat(value.assigneeId()).isEqualTo(7L);
            assertThat(value.priority()).isEqualTo("MEDIUM");
        });

        assertThat(meeting.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(meeting.getSummary()).isEqualTo("분석 요약");
        assertThat(meeting.getTopics()).containsExactly("API", "일정");
        assertThat(meeting.getDecisions()).singleElement().satisfies(decision ->
                assertThat(decision).containsEntry("content", "API를 먼저 구현한다.")
        );
        assertThat(meeting.getUnresolvedItems()).containsExactly("배포일 확정");
        verify(suggestionRepository).deleteAllByMeeting_Id(20L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MeetingTaskSuggestion>> suggestionCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(suggestionRepository).saveAll(suggestionCaptor.capture());
        assertThat(suggestionCaptor.getValue()).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.getSuggestionType()).isEqualTo("UPDATE");
            assertThat(suggestion.getTargetTaskId()).isEqualTo(30L);
            assertThat(suggestion.getReviewStatus()).isEqualTo("PENDING");
            assertThat(suggestion.getProposedData()).containsKeys(
                    "actionType",
                    "targetTaskId",
                    "title",
                    "description",
                    "assigneeId",
                    "dueDate",
                    "changes",
                    "evidenceSegmentIds",
                    "requiresReview",
                    "reason"
            );
            assertThat(suggestion.getProposedData().get("dueDate"))
                    .isEqualTo("2026-11-30");
        });
    }

    @Test
    void 잘못된_AI_응답이면_FAILED와_사유를_저장하고_제안은_저장하지_않는다() {
        Meeting meeting = meeting(MeetingProcessingStatus.STT_COMPLETED);
        ProjectMember member = member(7L, "다은", "BACKEND");
        when(meetingRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(meeting));
        when(projectMemberRepository.findAllWithUserByProjectId(10L))
                .thenReturn(List.of(member));
        when(taskRepository.findAllWithDetailsByProjectId(10L)).thenReturn(List.of());
        AiMeetingAnalyzeResponse invalidResponse = new AiMeetingAnalyzeResponse(
                999L,
                10L,
                "요약",
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(analysisClient.analyze(any())).thenReturn(invalidResponse);

        service.analyze(20L);

        assertThat(meeting.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(meeting.getFailureReason())
                .isEqualTo("AI 회의 분석 서버가 유효하지 않은 응답을 반환했습니다.");
        verify(suggestionRepository, never()).deleteAllByMeeting_Id(any());
        verify(suggestionRepository, never()).saveAll(any());
    }

    @Test
    void AI_연결_오류의_사유를_저장한다() {
        Meeting meeting = meeting(MeetingProcessingStatus.STT_COMPLETED);
        when(meetingRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(meeting));
        when(projectMemberRepository.findAllWithUserByProjectId(10L)).thenReturn(List.of());
        when(taskRepository.findAllWithDetailsByProjectId(10L)).thenReturn(List.of());
        when(analysisClient.analyze(any())).thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI 회의 분석 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
        ));

        service.analyze(20L);

        assertThat(meeting.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(meeting.getFailureReason())
                .isEqualTo("AI 회의 분석 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Test
    void 이미_완료된_회의를_재호출해도_제안을_중복_생성하지_않는다() {
        Meeting meeting = meeting(MeetingProcessingStatus.COMPLETED);
        when(meetingRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(meeting));

        service.analyze(20L);

        verify(analysisClient, never()).analyze(any());
        verify(suggestionRepository, never()).saveAll(any());
    }

    private Meeting meeting(MeetingProcessingStatus status) {
        Project project = new Project();
        project.setId(10L);
        Meeting meeting = new Meeting();
        meeting.setId(20L);
        meeting.setProject(project);
        meeting.setHeldAt(Instant.parse("2026-11-23T16:30:00Z"));
        meeting.setProcessingStatus(status.name());
        meeting.setTranscriptLanguage("ko-KR");
        meeting.setTranscript("회의 전체 내용");
        meeting.setTranscriptSegments(List.of(Map.of(
                "segmentId", "segment-1",
                "speakerId", "speaker-1",
                "startMs", 0L,
                "endMs", 1500L,
                "text", "첫 번째 발언"
        )));
        return meeting;
    }

    private ProjectMember member(Long userId, String nickname, String role) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        ProjectMember member = new ProjectMember();
        member.setId(70L);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private Task task(Long taskId, ProjectMember assignee) {
        Task task = new Task();
        task.setId(taskId);
        task.setTitle("기존 API 업무");
        task.setDescription("기존 설명");
        task.setAssigneeMember(assignee);
        task.setStartDate(LocalDate.of(2026, 11, 20));
        task.setDueDate(LocalDate.of(2026, 11, 28));
        task.setPriority("HIGH");
        task.setStatus("IN_PROGRESS");
        return task;
    }

    private AiMeetingAnalyzeResponse successResponse() {
        return new AiMeetingAnalyzeResponse(
                20L,
                10L,
                "분석 요약",
                List.of("API", "일정"),
                List.of(new AiMeetingAnalyzeResponse.Decision(
                        "API를 먼저 구현한다.",
                        List.of("segment-1")
                )),
                List.of("배포일 확정"),
                List.of(new AiMeetingAnalyzeResponse.ActionItem(
                        "UPDATE",
                        30L,
                        "API 업무 보완",
                        "분석에서 제안된 설명",
                        7L,
                        LocalDate.of(2026, 11, 30),
                        new AiMeetingAnalyzeResponse.TaskChanges(
                                "기존 API 업무 수정",
                                "변경 설명",
                                7L,
                                LocalDate.of(2026, 11, 24),
                                LocalDate.of(2026, 11, 30),
                                "URGENT"
                        ),
                        List.of("segment-1"),
                        true,
                        "회의에서 일정 변경을 합의함"
                ))
        );
    }
}
