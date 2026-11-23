package kr.co.daeun.flowmatebe.domain.project.controller;

import jakarta.validation.Valid;
import kr.co.daeun.flowmatebe.domain.project.dto.request.GenerateIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.request.RegisterIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.request.CreateProjectRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.response.CreateProjectResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.GenerateIssueDraftsResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectDetailResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectListResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMeetingsResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMembersResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectRepositoriesResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectTasksResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.RegisterIssueDraftsResponse;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectDetailService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectIssueDraftService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectIssueRegistrationService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectCreationService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectService;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskRequest;
import kr.co.daeun.flowmatebe.domain.task.dto.response.CreateTaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectDetailService projectDetailService;
    private final ProjectIssueDraftService projectIssueDraftService;
    private final ProjectIssueRegistrationService projectIssueRegistrationService;
    private final ProjectCreationService projectCreationService;

    @PostMapping
    public ResponseEntity<CreateProjectResponse> createProject(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        CreateProjectResponse response = projectCreationService.create(userId(jwt), request);
        return ResponseEntity
                .created(URI.create("/api/projects/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ProjectListResponse> getMyProjects(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(projectService.getMyProjects(userId(jwt)));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(
                projectDetailService.getProjectDetail(userId(jwt), projectId)
        );
    }

    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<ProjectTasksResponse> getProjectTasks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(
                projectDetailService.getProjectTasks(userId(jwt), projectId)
        );
    }

    @GetMapping("/{projectId}/meetings")
    public ResponseEntity<ProjectMeetingsResponse> getProjectMeetings(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(
                projectDetailService.getProjectMeetings(userId(jwt), projectId)
        );
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<ProjectMembersResponse> getProjectMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(
                projectDetailService.getProjectMembers(userId(jwt), projectId)
        );
    }

    @GetMapping("/{projectId}/repositories")
    public ResponseEntity<ProjectRepositoriesResponse> getProjectRepositories(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(
                projectDetailService.getProjectRepositories(userId(jwt), projectId)
        );
    }

    @PostMapping("/issue-drafts")
    public ResponseEntity<GenerateIssueDraftsResponse> generateIssueDrafts(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateIssueDraftsRequest request
    ) {
        return ResponseEntity.ok(
                projectIssueDraftService.generate(userId(jwt), request)
        );
    }

    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<RegisterIssueDraftsResponse> registerIssueDrafts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @Valid @RequestBody RegisterIssueDraftsRequest request
    ) {
        RegisterIssueDraftsResponse response = projectIssueRegistrationService.register(
                userId(jwt),
                projectId,
                request
        );
        return ResponseEntity
                .created(URI.create("/api/projects/" + projectId + "/tasks"))
                .body(response);
    }

    @PostMapping("/{projectId}/tasks/manual")
    public ResponseEntity<CreateTaskResponse> createManualTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        CreateTaskResponse response = projectIssueRegistrationService.createManualTask(
                userId(jwt),
                projectId,
                request
        );
        return ResponseEntity
                .created(URI.create("/api/tasks/" + response.taskId()))
                .body(response);
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
