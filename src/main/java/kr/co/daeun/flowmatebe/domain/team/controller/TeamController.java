package kr.co.daeun.flowmatebe.domain.team.controller;

import jakarta.validation.Valid;
import kr.co.daeun.flowmatebe.domain.team.dto.request.CreateTeamRequest;
import kr.co.daeun.flowmatebe.domain.team.dto.response.CreateTeamResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamDetailResponse;
import kr.co.daeun.flowmatebe.domain.team.dto.response.TeamListResponse;
import kr.co.daeun.flowmatebe.domain.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<TeamListResponse> getTeams(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(teamService.getTeams(userId(jwt)));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getTeamDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long teamId
    ) {
        return ResponseEntity.ok(teamService.getTeamDetail(userId(jwt), teamId));
    }

    @PostMapping
    public ResponseEntity<CreateTeamResponse> createTeam(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        CreateTeamResponse response = teamService.createTeam(userId(jwt), request);
        return ResponseEntity
                .created(URI.create("/api/teams/" + response.id()))
                .body(response);
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
