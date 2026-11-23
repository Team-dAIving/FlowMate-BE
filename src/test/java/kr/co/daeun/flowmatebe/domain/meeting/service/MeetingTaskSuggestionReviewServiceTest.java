package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.dto.request.ApproveMeetingTaskSuggestionRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.UpdateMeetingTaskSuggestionRequest;
import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingTaskSuggestionRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectIssueRegistrationService;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskRequest;
import kr.co.daeun.flowmatebe.domain.task.dto.response.CreateTaskResponse;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingTaskSuggestionReviewServiceTest {

    private final MeetingTaskSuggestionRepository suggestionRepository =
            mock(MeetingTaskSuggestionRepository.class);
    private final ProjectMemberRepository memberRepository =
            mock(ProjectMemberRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ProjectIssueRegistrationService issueRegistrationService =
            mock(ProjectIssueRegistrationService.class);
    private final MeetingTaskSuggestionReviewService service =
            new MeetingTaskSuggestionReviewService(
                    suggestionRepository,
                    memberRepository,
                    taskRepository,
                    issueRegistrationService
            );

    @Test
    void 검토_필요_제안을_신규_업무로_수정한다() {
        MeetingTaskSuggestion suggestion = suggestion("NEEDS_REVIEW");
        when(suggestionRepository.findAccessibleByIdForUpdate(40L, 20L, 1L))
                .thenReturn(Optional.of(suggestion));
        when(memberRepository.findByProject_IdAndUser_Id(10L, 7L))
                .thenReturn(Optional.of(member(7L)));

        var response = service.update(
                1L,
                20L,
                40L,
                new UpdateMeetingTaskSuggestionRequest(
                        "CREATE",
                        null,
                        "신규 업무",
                        "업무 설명",
                        7L,
                        LocalDate.of(2026, 11, 30),
                        "HIGH"
                )
        );

        assertThat(response.reviewStatus()).isEqualTo("PENDING");
        assertThat(response.actionType()).isEqualTo("CREATE");
        assertThat(suggestion.getSuggestionType()).isEqualTo("CREATE");
        assertThat(suggestion.getProposedData())
                .containsEntry("title", "신규 업무")
                .containsEntry("priority", "HIGH")
                .containsEntry("requiresReview", false);
        assertThat(suggestion.getProposedData().get("evidenceSegmentIds"))
                .isEqualTo(List.of("seg-1"));
    }

    @Test
    void 신규_업무_제안을_승인하면_Task와_GitHub_Issue를_생성한다() {
        MeetingTaskSuggestion suggestion = suggestion("CREATE");
        suggestion.setProposedData(new LinkedHashMap<>(Map.of(
                "title", "신규 업무",
                "description", "업무 설명",
                "assigneeId", 7L,
                "dueDate", "2026-11-30",
                "priority", "HIGH"
        )));
        when(suggestionRepository.findAccessibleByIdForUpdate(40L, 20L, 1L))
                .thenReturn(Optional.of(suggestion));
        when(issueRegistrationService.createManualTask(any(), any(), any()))
                .thenReturn(new CreateTaskResponse(
                        100L,
                        "신규 업무",
                        true,
                        "SYNCED",
                        500L,
                        31,
                        "https://github.test/issues/31",
                        null
                ));

        var response = service.approve(
                1L,
                20L,
                40L,
                new ApproveMeetingTaskSuggestionRequest(true, 500L, List.of("meeting"))
        );

        ArgumentCaptor<CreateTaskRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(issueRegistrationService).createManualTask(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                requestCaptor.capture()
        );
        assertThat(requestCaptor.getValue().title()).isEqualTo("신규 업무");
        assertThat(requestCaptor.getValue().priority()).isEqualTo("HIGH");
        assertThat(requestCaptor.getValue().createGithubIssue()).isTrue();
        assertThat(suggestion.getReviewStatus()).isEqualTo("APPROVED");
        assertThat(suggestion.getProposedData()).containsEntry("resultTaskId", 100L);
        assertThat(response.taskId()).isEqualTo(100L);
        assertThat(response.githubIssueNumber()).isEqualTo(31);
    }

    @Test
    void 기존_업무_수정_제안을_승인하면_Task를_수정한다() {
        MeetingTaskSuggestion suggestion = suggestion("UPDATE");
        suggestion.setTargetTaskId(100L);
        suggestion.setProposedData(Map.of(
                "changes", Map.of(
                        "title", "수정된 업무",
                        "description", "수정 설명",
                        "assigneeId", 7L,
                        "dueDate", "2026-11-29",
                        "priority", "URGENT"
                )
        ));
        ProjectMember assignee = member(7L);
        Task task = new Task();
        task.setId(100L);
        task.setProject(suggestion.getMeeting().getProject());
        task.setTitle("기존 업무");
        task.setStartDate(LocalDate.of(2026, 11, 24));
        when(suggestionRepository.findAccessibleByIdForUpdate(40L, 20L, 1L))
                .thenReturn(Optional.of(suggestion));
        when(taskRepository.findByIdAndProject_Id(100L, 10L))
                .thenReturn(Optional.of(task));
        when(memberRepository.findByProject_IdAndUser_Id(10L, 7L))
                .thenReturn(Optional.of(assignee));

        var response = service.approve(
                1L,
                20L,
                40L,
                new ApproveMeetingTaskSuggestionRequest(false, null, List.of())
        );

        assertThat(task.getTitle()).isEqualTo("수정된 업무");
        assertThat(task.getDescription()).isEqualTo("수정 설명");
        assertThat(task.getAssigneeMember()).isSameAs(assignee);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 29));
        assertThat(task.getPriority()).isEqualTo("URGENT");
        assertThat(response.taskId()).isEqualTo(100L);
        assertThat(suggestion.getReviewStatus()).isEqualTo("APPROVED");
        verify(issueRegistrationService, never()).createManualTask(any(), any(), any());
    }

    @Test
    void 제안을_거절하면_Task를_변경하지_않는다() {
        MeetingTaskSuggestion suggestion = suggestion("CREATE");
        when(suggestionRepository.findAccessibleByIdForUpdate(40L, 20L, 1L))
                .thenReturn(Optional.of(suggestion));

        var response = service.reject(1L, 20L, 40L);

        assertThat(response.reviewStatus()).isEqualTo("REJECTED");
        assertThat(suggestion.getReviewStatus()).isEqualTo("REJECTED");
        verify(taskRepository, never()).save(any());
        verify(issueRegistrationService, never()).createManualTask(any(), any(), any());
    }

    @Test
    void 이미_처리한_제안은_중복_승인할_수_없다() {
        MeetingTaskSuggestion suggestion = suggestion("CREATE");
        suggestion.setReviewStatus("APPROVED");
        when(suggestionRepository.findAccessibleByIdForUpdate(40L, 20L, 1L))
                .thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> service.approve(
                1L,
                20L,
                40L,
                new ApproveMeetingTaskSuggestionRequest(false, null, List.of())
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 처리된 업무 제안");
    }

    private MeetingTaskSuggestion suggestion(String type) {
        Project project = new Project();
        project.setId(10L);
        project.setStartDate(LocalDate.of(2026, 11, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        Meeting meeting = new Meeting();
        meeting.setId(20L);
        meeting.setProject(project);
        MeetingTaskSuggestion suggestion = new MeetingTaskSuggestion();
        suggestion.setId(40L);
        suggestion.setMeeting(meeting);
        suggestion.setSuggestionType(type);
        suggestion.setReviewStatus("PENDING");
        suggestion.setProposedData(Map.of(
                "evidenceSegmentIds", List.of("seg-1"),
                "reason", "회의 근거"
        ));
        return suggestion;
    }

    private ProjectMember member(Long userId) {
        User user = new User();
        user.setId(userId);
        ProjectMember member = new ProjectMember();
        member.setUser(user);
        return member;
    }
}
