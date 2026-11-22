package kr.co.daeun.flowmatebe.domain.task.service;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskCommentRequest;
import kr.co.daeun.flowmatebe.domain.task.dto.response.TaskCommentResponse;
import kr.co.daeun.flowmatebe.domain.task.dto.response.TaskDetailResponse;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.entity.TaskComment;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskCommentRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskDetailResponse getTaskDetail(Long currentUserId, Long taskId) {
        Task task = getAccessibleTask(currentUserId, taskId);
        List<TaskCommentResponse> comments = taskCommentRepository
                .findAllWithAuthorByTaskId(taskId)
                .stream()
                .map(comment -> TaskCommentResponse.of(comment, currentUserId))
                .toList();
        return TaskDetailResponse.of(task, comments);
    }

    @Transactional
    public TaskCommentResponse createComment(
            Long currentUserId,
            Long taskId,
            CreateTaskCommentRequest request
    ) {
        Task task = getAccessibleTask(currentUserId, taskId);
        Long projectId = task.getProject().getId();
        ProjectMember author = projectMemberRepository
                .findByProject_IdAndUser_Id(projectId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "프로젝트에 참여한 팀원만 댓글을 작성할 수 있습니다."
                ));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(author)
                .content(request.content().strip())
                .build();
        TaskComment savedComment = taskCommentRepository.save(comment);
        return TaskCommentResponse.of(savedComment, currentUserId);
    }

    @Transactional
    public void deleteTask(Long currentUserId, Long taskId) {
        Task task = getAccessibleTask(currentUserId, taskId);
        taskCommentRepository.deleteAllByTaskId(taskId);
        taskRepository.delete(task);
    }

    private Task getAccessibleTask(Long currentUserId, Long taskId) {
        return taskRepository.findAccessibleDetailByIdAndUserId(taskId, currentUserId)
                .orElseThrow(() -> taskNotFound(taskId));
    }

    private ResponseStatusException taskNotFound(Long taskId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "업무를 찾을 수 없습니다: " + taskId
        );
    }
}
