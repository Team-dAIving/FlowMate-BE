package kr.co.daeun.flowmatebe.domain.github.controller;

import kr.co.daeun.flowmatebe.domain.github.dto.response.GithubAppInstallResponse;
import kr.co.daeun.flowmatebe.domain.github.dto.response.GithubInstallationListResponse;
import kr.co.daeun.flowmatebe.domain.github.service.GithubAppInstallationService;
import kr.co.daeun.flowmatebe.domain.github.service.GithubInstallationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubAppController {

    private final GithubAppInstallationService githubAppInstallationService;
    private final GithubInstallationQueryService githubInstallationQueryService;

    @GetMapping("/installations")
    public ResponseEntity<GithubInstallationListResponse> getInstallations(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                githubInstallationQueryService.getMyInstallations(userId(jwt))
        );
    }

    @PostMapping("/installations/start")
    public ResponseEntity<GithubAppInstallResponse> startInstallation(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                githubAppInstallationService.createInstallUrl(userId(jwt))
        );
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
