package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.dto.request.ApproveMeetingTaskSuggestionRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.request.UpdateMeetingTaskSuggestionRequest;
import kr.co.daeun.flowmatebe.domain.meeting.dto.response.MeetingTaskSuggestionReviewResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingTaskSuggestionReviewService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final MeetingTaskSuggestionRepository suggestionRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final ProjectIssueRegistrationService issueRegistrationService;

    @Transactional
    public MeetingTaskSuggestionReviewResponse update(
            Long currentUserId,
            Long meetingId,
            Long suggestionId,
            UpdateMeetingTaskSuggestionRequest request
    ) {
        MeetingTaskSuggestion suggestion = getPendingSuggestion(
                currentUserId,
                meetingId,
                suggestionId
        );
        Project project = suggestion.getMeeting().getProject();
        String actionType = request.actionType().toUpperCase(Locale.ROOT);
        validateTargetTask(actionType, request.targetTaskId(), project.getId());
        validateAssignee(request.assigneeId(), project.getId());
        validateDueDate(project, request.dueDate(), request.title());

        Map<String, Object> previous = suggestion.getProposedData() == null
                ? Map.of()
                : suggestion.getProposedData();
        Map<String, Object> proposedData = new LinkedHashMap<>();
        proposedData.put("actionType", actionType);
        proposedData.put("targetTaskId", request.targetTaskId());
        proposedData.put("title", request.title().strip());
        proposedData.put("description", request.description().strip());
        proposedData.put("assigneeId", request.assigneeId());
        proposedData.put("dueDate", request.dueDate().toString());
        proposedData.put("priority", request.priority().toUpperCase(Locale.ROOT));
        proposedData.put("changes", "UPDATE".equals(actionType)
                ? updateChanges(request)
                : null);
        proposedData.put("evidenceSegmentIds", copyStringList(
                previous.get("evidenceSegmentIds")
        ));
        proposedData.put("requiresReview", false);
        proposedData.put("reason", optionalString(previous.get("reason"), ""));

        suggestion.setSuggestionType(actionType);
        suggestion.setTargetTaskId(request.targetTaskId());
        suggestion.setProposedData(proposedData);
        return response(suggestion, null);
    }

    @Transactional
    public MeetingTaskSuggestionReviewResponse approve(
            Long currentUserId,
            Long meetingId,
            Long suggestionId,
            ApproveMeetingTaskSuggestionRequest request
    ) {
        MeetingTaskSuggestion suggestion = getPendingSuggestion(
                currentUserId,
                meetingId,
                suggestionId
        );
        String actionType = suggestion.getSuggestionType();
        CreateTaskResponse taskResponse;
        if ("CREATE".equals(actionType) || "NEEDS_REVIEW".equals(actionType)) {
            taskResponse = createTask(currentUserId, suggestion, request);
            if ("NEEDS_REVIEW".equals(actionType)) {
                suggestion.setSuggestionType("CREATE");
                suggestion.setTargetTaskId(null);
            }
        } else if ("UPDATE".equals(actionType)) {
            if (Boolean.TRUE.equals(request.createGithubIssue())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "기존 업무 수정 승인에서는 새 GitHub Issue를 생성할 수 없습니다."
                );
            }
            Task task = updateTask(suggestion);
            taskResponse = new CreateTaskResponse(
                    task.getId(),
                    task.getTitle(),
                    false,
                    task.getGithubSyncStatus(),
                    task.getGithubRepository() == null
                            ? null
                            : task.getGithubRepository().getGithubRepositoryId(),
                    task.getGithubIssueNumber(),
                    null,
                    null
            );
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "승인할 수 없는 업무 제안 유형입니다."
            );
        }

        suggestion.setReviewStatus(APPROVED);
        Map<String, Object> proposedData = mutableData(suggestion);
        proposedData.put("resultTaskId", taskResponse.taskId());
        suggestion.setProposedData(proposedData);
        return response(suggestion, taskResponse);
    }

    @Transactional
    public MeetingTaskSuggestionReviewResponse reject(
            Long currentUserId,
            Long meetingId,
            Long suggestionId
    ) {
        MeetingTaskSuggestion suggestion = getPendingSuggestion(
                currentUserId,
                meetingId,
                suggestionId
        );
        suggestion.setReviewStatus(REJECTED);
        return response(suggestion, null);
    }

    private CreateTaskResponse createTask(
            Long currentUserId,
            MeetingTaskSuggestion suggestion,
            ApproveMeetingTaskSuggestionRequest request
    ) {
        Map<String, Object> data = requiredData(suggestion);
        CreateTaskRequest taskRequest = new CreateTaskRequest(
                requiredString(data, "title"),
                optionalString(data.get("description"), ""),
                optionalString(data.get("priority"), "MEDIUM"),
                optionalLong(data.get("assigneeId")),
                requiredDate(data, "dueDate"),
                null,
                request.labels(),
                request.createGithubIssue(),
                request.githubRepositoryId()
        );
        return issueRegistrationService.createManualTask(
                currentUserId,
                suggestion.getMeeting().getProject().getId(),
                taskRequest
        );
    }

    private Task updateTask(MeetingTaskSuggestion suggestion) {
        Project project = suggestion.getMeeting().getProject();
        Long targetTaskId = suggestion.getTargetTaskId();
        if (targetTaskId == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "수정할 기존 업무를 먼저 선택해 주세요."
            );
        }
        Task task = taskRepository.findByIdAndProject_Id(targetTaskId, project.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "수정할 업무를 찾을 수 없습니다."
                ));
        Map<String, Object> data = requiredData(suggestion);
        Map<String, Object> changes = nestedMap(data.get("changes"));
        if (changes == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "수정할 업무 내용을 먼저 확정해 주세요."
            );
        }

        String title = optionalString(changes.get("title"), null);
        if (title != null) {
            if (title.isBlank()) {
                throw invalidProposal();
            }
            task.setTitle(title.strip());
        }
        String description = optionalString(changes.get("description"), null);
        if (description != null) {
            task.setDescription(description.strip());
        }
        if (changes.containsKey("assigneeId") && changes.get("assigneeId") != null) {
            task.setAssigneeMember(validateAssignee(
                    optionalLong(changes.get("assigneeId")),
                    project.getId()
            ));
        }
        LocalDate dueDate = optionalDate(changes.get("dueDate"));
        if (dueDate != null) {
            validateDueDate(project, dueDate, task.getTitle());
            if (task.getStartDate() != null && task.getStartDate().isAfter(dueDate)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "업무 마감일은 시작일보다 빠를 수 없습니다."
                );
            }
            task.setDueDate(dueDate);
        }
        String priority = optionalString(changes.get("priority"), null);
        if (priority != null) {
            task.setPriority(priority.toUpperCase(Locale.ROOT));
        }
        return task;
    }

    private void validateTargetTask(String actionType, Long targetTaskId, Long projectId) {
        if ("CREATE".equals(actionType)) {
            if (targetTaskId != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "신규 업무 제안에는 대상 업무 ID를 지정할 수 없습니다."
                );
            }
            return;
        }
        if (targetTaskId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기존 업무 수정 제안에는 대상 업무 ID가 필요합니다."
            );
        }
        if (taskRepository.findByIdAndProject_Id(targetTaskId, projectId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "해당 프로젝트의 업무가 아닙니다."
            );
        }
    }

    private ProjectMember validateAssignee(Long assigneeId, Long projectId) {
        if (assigneeId == null) {
            return null;
        }
        return projectMemberRepository.findByProject_IdAndUser_Id(projectId, assigneeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "해당 프로젝트의 멤버가 아닌 담당자입니다."
                ));
    }

    private void validateDueDate(Project project, LocalDate dueDate, String title) {
        if ((project.getStartDate() != null && dueDate.isBefore(project.getStartDate()))
                || (project.getEndDate() != null && dueDate.isAfter(project.getEndDate()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업무 마감일은 프로젝트 기간 안에 있어야 합니다: " + title
            );
        }
    }

    private MeetingTaskSuggestion getPendingSuggestion(
            Long currentUserId,
            Long meetingId,
            Long suggestionId
    ) {
        MeetingTaskSuggestion suggestion = suggestionRepository
                .findAccessibleByIdForUpdate(suggestionId, meetingId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "업무 제안을 찾을 수 없습니다."
                ));
        if (!PENDING.equals(suggestion.getReviewStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 처리된 업무 제안입니다: " + suggestion.getReviewStatus()
            );
        }
        return suggestion;
    }

    private Map<String, Object> updateChanges(UpdateMeetingTaskSuggestionRequest request) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("title", request.title().strip());
        changes.put("description", request.description().strip());
        changes.put("assigneeId", request.assigneeId());
        changes.put("startDate", null);
        changes.put("dueDate", request.dueDate().toString());
        changes.put("priority", request.priority().toUpperCase(Locale.ROOT));
        return changes;
    }

    private MeetingTaskSuggestionReviewResponse response(
            MeetingTaskSuggestion suggestion,
            CreateTaskResponse task
    ) {
        return new MeetingTaskSuggestionReviewResponse(
                suggestion.getId(),
                suggestion.getReviewStatus(),
                suggestion.getSuggestionType(),
                task == null ? null : task.taskId(),
                suggestion.getProposedData(),
                task == null ? null : task.githubIssueRequested(),
                task == null ? null : task.githubSyncStatus(),
                task == null ? null : task.githubRepositoryId(),
                task == null ? null : task.githubIssueNumber(),
                task == null ? null : task.githubIssueUrl(),
                task == null ? null : task.githubError()
        );
    }

    private Map<String, Object> mutableData(MeetingTaskSuggestion suggestion) {
        return suggestion.getProposedData() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(suggestion.getProposedData());
    }

    private Map<String, Object> requiredData(MeetingTaskSuggestion suggestion) {
        if (suggestion.getProposedData() == null) {
            throw invalidProposal();
        }
        return suggestion.getProposedData();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?> raw)) {
            throw invalidProposal();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private String requiredString(Map<String, Object> data, String key) {
        String value = optionalString(data.get(key), null);
        if (value == null || value.isBlank()) {
            throw invalidProposal();
        }
        return value.strip();
    }

    private String optionalString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String string)) {
            throw invalidProposal();
        }
        return string;
    }

    private Long optionalLong(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw invalidProposal();
        }
        return number.longValue();
    }

    private LocalDate requiredDate(Map<String, Object> data, String key) {
        LocalDate value = optionalDate(data.get(key));
        if (value == null) {
            throw invalidProposal();
        }
        return value;
    }

    private LocalDate optionalDate(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return value instanceof LocalDate date
                    ? date
                    : LocalDate.parse(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw invalidProposal();
        }
    }

    private List<String> copyStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)
                || list.stream().anyMatch(item -> !(item instanceof String))) {
            throw invalidProposal();
        }
        return list.stream().map(String.class::cast).toList();
    }

    private ResponseStatusException invalidProposal() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "업무 제안 데이터가 올바르지 않습니다. 내용을 수정한 뒤 다시 승인해 주세요."
        );
    }
}
