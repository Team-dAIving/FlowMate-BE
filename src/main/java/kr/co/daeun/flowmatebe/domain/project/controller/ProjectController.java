package kr.co.daeun.flowmatebe.domain.project.controller;

import jakarta.validation.Valid;
import kr.co.daeun.flowmatebe.domain.project.dto.request.GenerateIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.request.CreateProjectRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.response.CreateProjectResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.GenerateIssueDraftsResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectDetailResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectListResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMeetingsResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectMembersResponse;
import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectTasksResponse;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectDetailService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectIssueDraftService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectCreationService;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectService;
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

    @PostMapping("/issue-drafts")
    public ResponseEntity<GenerateIssueDraftsResponse> generateIssueDrafts(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateIssueDraftsRequest request
    ) {
        return ResponseEntity.ok(
                projectIssueDraftService.generate(userId(jwt), request)
        );
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
