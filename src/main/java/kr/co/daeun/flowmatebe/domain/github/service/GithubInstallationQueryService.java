package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.dto.response.GithubInstallationListResponse;
import kr.co.daeun.flowmatebe.domain.github.entity.TeamGithubInstallation;
import kr.co.daeun.flowmatebe.domain.github.repository.TeamGithubInstallationRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationRepositories;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubInstallationQueryService {

    private final TeamRepository teamRepository;
    private final TeamGithubInstallationRepository teamInstallationRepository;
    private final GithubInstallationClient installationClient;

    public GithubInstallationListResponse getTeamInstallation(
            Long currentUserId,
            Long teamId
    ) {
        teamRepository.findByIdAndMemberUserId(teamId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "GitHub 연결을 조회할 팀을 찾을 수 없습니다."
                ));
        return teamInstallationRepository.findByTeam_Id(teamId)
                .map(this::toResponse)
                .orElseGet(() -> new GithubInstallationListResponse(List.of()));
    }

    private GithubInstallationListResponse toResponse(TeamGithubInstallation installation) {
        GithubInstallationRepositories repositories =
                installationClient.listRepositories(installation.getInstallationId());
        return new GithubInstallationListResponse(List.of(
                new GithubInstallationListResponse.Installation(
                        installation.getInstallationId(),
                        installation.getAccountLogin(),
                        "Organization",
                        repositories.repositories()
                )
        ));
    }
}
