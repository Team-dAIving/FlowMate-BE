package kr.co.daeun.flowmatebe.domain.project.controller;

import kr.co.daeun.flowmatebe.domain.project.dto.response.ProjectListResponse;
import kr.co.daeun.flowmatebe.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<ProjectListResponse> getMyProjects(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(projectService.getMyProjects(userId(jwt)));
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
