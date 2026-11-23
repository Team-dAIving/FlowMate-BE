package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingProcessingStatus;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingTaskSuggestionRepository;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.global.ai.client.AiMeetingAnalysisClient;
import kr.co.daeun.flowmatebe.global.ai.dto.AiMeetingAnalyzeRequest;
import kr.co.daeun.flowmatebe.global.ai.dto.AiMeetingAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingAnalysisService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<String> ACTION_TYPES = Set.of(
            "CREATE", "UPDATE", "NEEDS_REVIEW"
    );
    private static final Set<String> TASK_PRIORITIES = Set.of(
            "LOW", "MEDIUM", "HIGH", "URGENT"
    );
    private static final Set<String> TASK_STATUSES = Set.of(
            "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE", "BLOCKED"
    );

    private final MeetingRepository meetingRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final MeetingTaskSuggestionRepository suggestionRepository;
    private final AiMeetingAnalysisClient analysisClient;

    @Transactional
    public void analyze(Long meetingId) {
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
        if (meeting == null
                || !MeetingProcessingStatus.STT_COMPLETED.name()
                .equals(meeting.getProcessingStatus())) {
            return;
        }

        meeting.setProcessingStatus(MeetingProcessingStatus.ANALYZING.name());
        meeting.setFailureReason(null);
        try {
            AnalysisContext context = createContext(meeting);
            AiMeetingAnalyzeResponse response = analysisClient.analyze(context.request());
            applyResponse(meeting, context, response);
        } catch (RuntimeException exception) {
            log.warn("회의 분석에 실패했습니다. meetingId={}", meetingId, exception);
            meeting.setProcessingStatus(MeetingProcessingStatus.FAILED.name());
            meeting.setFailureReason(failureReason(exception));
        }
    }

    private AnalysisContext createContext(Meeting meeting) {
        if (meeting.getHeldAt() == null
                || meeting.getTranscriptLanguage() == null
                || meeting.getTranscriptLanguage().isBlank()
                || meeting.getTranscript() == null
                || meeting.getTranscriptSegments() == null) {
            throw invalidSourceData();
        }

        Long projectId = meeting.getProject().getId();
        List<ProjectMember> projectMembers =
                projectMemberRepository.findAllWithUserByProjectId(projectId);
        List<Task> tasks = taskRepository.findAllWithDetailsByProjectId(projectId);

        List<AiMeetingAnalyzeRequest.Segment> segments = toSegments(
                meeting.getTranscriptSegments()
        );
        Set<String> segmentIds = new LinkedHashSet<>();
        for (AiMeetingAnalyzeRequest.Segment segment : segments) {
            if (!segmentIds.add(segment.segmentId())) {
                throw invalidSourceData();
            }
        }

        List<AiMeetingAnalyzeRequest.Member> members = projectMembers.stream()
                .map(this::toMember)
                .toList();
        Set<Long> memberIds = projectMembers.stream()
                .map(ProjectMember::getUser)
                .map(User::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<AiMeetingAnalyzeRequest.ExistingTask> existingTasks = tasks.stream()
                .map(this::toExistingTask)
                .toList();
        Set<Long> taskIds = tasks.stream()
                .map(Task::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        AiMeetingAnalyzeRequest request = new AiMeetingAnalyzeRequest(
                meeting.getId(),
                projectId,
                meeting.getHeldAt().atZone(KOREA_ZONE).toLocalDate(),
                new AiMeetingAnalyzeRequest.Transcript(
                        meeting.getId(),
                        meeting.getTranscriptLanguage(),
                        meeting.getTranscript(),
                        segments
                ),
                members,
                existingTasks
        );
        return new AnalysisContext(request, memberIds, taskIds, segmentIds);
    }

    private List<AiMeetingAnalyzeRequest.Segment> toSegments(
            List<Map<String, Object>> source
    ) {
        List<AiMeetingAnalyzeRequest.Segment> segments = new ArrayList<>();
        for (Map<String, Object> value : source) {
            if (value == null) {
                throw invalidSourceData();
            }
            String segmentId = requiredString(value, "segmentId");
            String speakerId = optionalString(value, "speakerId");
            Long startMs = requiredLong(value, "startMs");
            Long endMs = requiredLong(value, "endMs");
            String text = requiredStringAllowEmpty(value, "text");
            if (startMs < 0 || endMs < startMs) {
                throw invalidSourceData();
            }
            segments.add(new AiMeetingAnalyzeRequest.Segment(
                    segmentId,
                    speakerId,
                    startMs,
                    endMs,
                    text
            ));
        }
        return List.copyOf(segments);
    }

    private AiMeetingAnalyzeRequest.Member toMember(ProjectMember member) {
        User user = member.getUser();
        return new AiMeetingAnalyzeRequest.Member(
                user.getId(),
                displayName(user),
                member.getRole() == null ? "" : member.getRole()
        );
    }

    private AiMeetingAnalyzeRequest.ExistingTask toExistingTask(Task task) {
        String priority = defaultValue(task.getPriority(), "MEDIUM");
        String status = defaultValue(task.getStatus(), "TODO");
        if (task.getId() == null
                || task.getTitle() == null
                || task.getTitle().isBlank()
                || !TASK_PRIORITIES.contains(priority)
                || !TASK_STATUSES.contains(status)) {
            throw invalidSourceData();
        }
        Long assigneeId = task.getAssigneeMember() == null
                ? null
                : task.getAssigneeMember().getUser().getId();
        return new AiMeetingAnalyzeRequest.ExistingTask(
                task.getId(),
                task.getTitle(),
                task.getDescription() == null ? "" : task.getDescription(),
                assigneeId,
                task.getStartDate(),
                task.getDueDate(),
                priority,
                status
        );
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void applyResponse(
            Meeting meeting,
            AnalysisContext context,
            AiMeetingAnalyzeResponse response
    ) {
        validateResponse(meeting, context, response);

        meeting.setSummary(response.summary());
        meeting.setTopics(List.copyOf(response.topics()));
        meeting.setDecisions(response.decisions().stream()
                .map(this::decisionData)
                .toList());
        meeting.setUnresolvedItems(List.copyOf(response.unresolvedItems()));

        suggestionRepository.deleteAllByMeeting_Id(meeting.getId());
        List<MeetingTaskSuggestion> suggestions = response.actionItems().stream()
                .map(actionItem -> toSuggestion(meeting, actionItem))
                .toList();
        suggestionRepository.saveAll(suggestions);

        meeting.setProcessingStatus(MeetingProcessingStatus.COMPLETED.name());
        meeting.setFailureReason(null);
    }

    private void validateResponse(
            Meeting meeting,
            AnalysisContext context,
            AiMeetingAnalyzeResponse response
    ) {
        if (response == null
                || !meeting.getId().equals(response.meetingId())
                || !meeting.getProject().getId().equals(response.projectId())
                || response.summary() == null
                || response.topics() == null
                || response.decisions() == null
                || response.unresolvedItems() == null
                || response.actionItems() == null
                || response.topics().stream().anyMatch(java.util.Objects::isNull)
                || response.unresolvedItems().stream().anyMatch(java.util.Objects::isNull)) {
            throw invalidAiResponse();
        }
        for (AiMeetingAnalyzeResponse.Decision decision : response.decisions()) {
            if (decision == null || decision.content() == null) {
                throw invalidAiResponse();
            }
            validateEvidenceIds(decision.evidenceSegmentIds(), context.segmentIds());
        }
        for (AiMeetingAnalyzeResponse.ActionItem actionItem : response.actionItems()) {
            if (actionItem == null
                    || !ACTION_TYPES.contains(actionItem.actionType())
                    || actionItem.title() == null
                    || actionItem.title().isBlank()
                    || (actionItem.targetTaskId() != null
                    && !context.taskIds().contains(actionItem.targetTaskId()))
                    || (actionItem.assigneeId() != null
                    && !context.memberIds().contains(actionItem.assigneeId()))) {
                throw invalidAiResponse();
            }
            validateEvidenceIds(actionItem.evidenceSegmentIds(), context.segmentIds());
            validateChanges(actionItem.changes(), context.memberIds());
        }
    }

    private void validateEvidenceIds(List<String> evidenceIds, Set<String> segmentIds) {
        if (evidenceIds == null
                || evidenceIds.stream().anyMatch(id -> id == null || !segmentIds.contains(id))) {
            throw invalidAiResponse();
        }
    }

    private void validateChanges(
            AiMeetingAnalyzeResponse.TaskChanges changes,
            Set<Long> memberIds
    ) {
        if (changes == null) {
            return;
        }
        if ((changes.assigneeId() != null && !memberIds.contains(changes.assigneeId()))
                || (changes.priority() != null
                && !TASK_PRIORITIES.contains(changes.priority()))) {
            throw invalidAiResponse();
        }
    }

    private Map<String, Object> decisionData(AiMeetingAnalyzeResponse.Decision decision) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("content", decision.content());
        value.put("evidenceSegmentIds", List.copyOf(decision.evidenceSegmentIds()));
        return value;
    }

    private MeetingTaskSuggestion toSuggestion(
            Meeting meeting,
            AiMeetingAnalyzeResponse.ActionItem actionItem
    ) {
        MeetingTaskSuggestion suggestion = new MeetingTaskSuggestion();
        suggestion.setMeeting(meeting);
        suggestion.setSuggestionType(actionItem.actionType());
        suggestion.setTargetTaskId(actionItem.targetTaskId());
        suggestion.setReviewStatus("PENDING");
        suggestion.setProposedData(proposedData(actionItem));
        return suggestion;
    }

    private Map<String, Object> proposedData(
            AiMeetingAnalyzeResponse.ActionItem actionItem
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("actionType", actionItem.actionType());
        value.put("targetTaskId", actionItem.targetTaskId());
        value.put("title", actionItem.title());
        value.put("description", actionItem.description() == null
                ? "" : actionItem.description());
        value.put("assigneeId", actionItem.assigneeId());
        value.put("dueDate", dateValue(actionItem.dueDate()));
        value.put("changes", changesData(actionItem.changes()));
        value.put("evidenceSegmentIds", List.copyOf(actionItem.evidenceSegmentIds()));
        value.put("requiresReview", actionItem.requiresReview() == null
                || actionItem.requiresReview());
        value.put("reason", actionItem.reason() == null ? "" : actionItem.reason());
        return value;
    }

    private Map<String, Object> changesData(AiMeetingAnalyzeResponse.TaskChanges changes) {
        if (changes == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("title", changes.title());
        value.put("description", changes.description());
        value.put("assigneeId", changes.assigneeId());
        value.put("startDate", dateValue(changes.startDate()));
        value.put("dueDate", dateValue(changes.dueDate()));
        value.put("priority", changes.priority());
        return value;
    }

    private String dateValue(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().strip();
        }
        if (user.getGithubLogin() != null && !user.getGithubLogin().isBlank()) {
            return user.getGithubLogin().strip();
        }
        return "사용자 " + user.getId();
    }

    private String requiredString(Map<String, Object> value, String key) {
        String result = requiredStringAllowEmpty(value, key);
        if (result.isBlank()) {
            throw invalidSourceData();
        }
        return result;
    }

    private String requiredStringAllowEmpty(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof String result)) {
            throw invalidSourceData();
        }
        return result;
    }

    private String optionalString(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String result)) {
            throw invalidSourceData();
        }
        return result;
    }

    private Long requiredLong(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof Number number)) {
            throw invalidSourceData();
        }
        return number.longValue();
    }

    private String failureReason(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException
                && responseStatusException.getReason() != null
                && !responseStatusException.getReason().isBlank()) {
            return responseStatusException.getReason().strip();
        }
        return "AI 회의 분석 중 오류가 발생했습니다.";
    }

    private ResponseStatusException invalidSourceData() {
        return new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "회의 분석 요청 데이터를 구성할 수 없습니다."
        );
    }

    private ResponseStatusException invalidAiResponse() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI 회의 분석 서버가 유효하지 않은 응답을 반환했습니다."
        );
    }

    private record AnalysisContext(
            AiMeetingAnalyzeRequest request,
            Set<Long> memberIds,
            Set<Long> taskIds,
            Set<String> segmentIds
    ) {
    }
}
