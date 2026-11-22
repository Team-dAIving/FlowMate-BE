package kr.co.daeun.flowmatebe.domain.task.controller;

import jakarta.validation.Valid;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskCommentRequest;
import kr.co.daeun.flowmatebe.domain.task.dto.response.TaskCommentResponse;
import kr.co.daeun.flowmatebe.domain.task.dto.response.TaskDetailResponse;
import kr.co.daeun.flowmatebe.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDetailResponse> getTaskDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(taskService.getTaskDetail(userId(jwt), taskId));
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<TaskCommentResponse> createComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateTaskCommentRequest request
    ) {
        TaskCommentResponse response = taskService.createComment(userId(jwt), taskId, request);
        return ResponseEntity
                .created(URI.create("/api/tasks/" + taskId + "/comments/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long taskId
    ) {
        taskService.deleteTask(userId(jwt), taskId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
