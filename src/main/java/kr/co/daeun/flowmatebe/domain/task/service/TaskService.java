package kr.co.daeun.flowmatebe.domain.task.service;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectTaskResponse;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskCommentRequest;
import kr.co.daeun.flowmatebe.domain.task.dto.response.TaskCommentResponse;
import kr.co.daeun.flowmatebe.domain.task.dto.response.TaskDetailResponse;
import kr.co.daeun.flowmatebe.domain.task.dto.response.MyProjectTasksResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;

    public MyProjectTasksResponse getMyProjectTasks(Long currentUserId) {
        var projects = projectRepository
                .findAllByMemberUserIdOrderByCreatedAtDesc(currentUserId);
        if (projects.isEmpty()) {
            return new MyProjectTasksResponse(List.of());
        }

        List<Long> projectIds = projects.stream()
                .map(project -> project.getId())
                .toList();
        List<Task> tasks = taskRepository.findAllWithDetailsByProjectIds(projectIds);
        Map<Long, Long> commentCounts = commentCounts(tasks);
        Map<Long, List<ProjectTaskResponse>> tasksByProjectId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getProject().getId(),
                        Collectors.mapping(
                                task -> ProjectTaskResponse.of(
                                        task,
                                        commentCounts.getOrDefault(task.getId(), 0L)
                                ),
                                Collectors.toList()
                        )
                ));

        return new MyProjectTasksResponse(projects.stream()
                .map(project -> new MyProjectTasksResponse.ProjectTasks(
                        project.getId(),
                        project.getName(),
                        project.getStatus(),
                        project.getStartDate(),
                        project.getEndDate(),
                        tasksByProjectId.getOrDefault(project.getId(), List.of())
                ))
                .toList());
    }

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

    private Map<Long, Long> commentCounts(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return Map.of();
        }
        return taskCommentRepository.countByTaskIds(tasks.stream().map(Task::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        TaskCommentRepository.TaskCommentCount::getTaskId,
                        TaskCommentRepository.TaskCommentCount::getCommentCount
                ));
    }
}
