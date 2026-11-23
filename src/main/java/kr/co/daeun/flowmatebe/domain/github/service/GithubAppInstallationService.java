package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.dto.response.GithubAppInstallResponse;
import kr.co.daeun.flowmatebe.domain.github.entity.TeamGithubInstallation;
import kr.co.daeun.flowmatebe.domain.github.repository.TeamGithubInstallationRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationMetadata;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GithubAppInstallationService {

    private static final String INSTALLATION_URL =
            "https://github.com/apps/{slug}/installations/new";
    private final GithubAppProperties githubAppProperties;
    private final GithubInstallationStateService stateService;
    private final TeamRepository teamRepository;
    private final TeamGithubInstallationRepository teamInstallationRepository;
    private final GithubInstallationClient installationClient;

    public GithubAppInstallResponse createInstallUrl(Long currentUserId, Long teamId) {
        String slug = githubAppProperties.slug();
        if (!StringUtils.hasText(slug)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub App이 설정되지 않았습니다."
            );
        }

        requireTeamMember(teamId, currentUserId);
        GithubInstallationStateService.InstallationState state =
                stateService.create(currentUserId, teamId);
        String installUrl = UriComponentsBuilder.fromUriString(INSTALLATION_URL)
                .queryParam("state", state.value())
                .buildAndExpand(slug.strip())
                .encode()
                .toUriString();

        return new GithubAppInstallResponse(installUrl, state.expiresAt());
    }

    @Transactional
    public void completeInstallation(
            Long currentUserId,
            Long installationId,
            String state
    ) {
        Long teamId = stateService.validate(state, currentUserId).teamId();
        Team team = requireTeamMember(teamId, currentUserId);
        GithubInstallationMetadata metadata = installationClient.getInstallation(installationId);
        if (!"Organization".equalsIgnoreCase(metadata.accountType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GitHub Organization installation만 팀에 연결할 수 있습니다."
            );
        }

        TeamGithubInstallation installation = teamInstallationRepository
                .findByTeam_Id(teamId)
                .orElseGet(TeamGithubInstallation::new);
        installation.setTeam(team);
        installation.setInstallationId(metadata.installationId());
        installation.setGithubAccountId(metadata.accountId());
        installation.setAccountLogin(metadata.accountLogin());
        teamInstallationRepository.save(installation);
    }

    private Team requireTeamMember(Long teamId, Long currentUserId) {
        return teamRepository.findByIdAndMemberUserId(teamId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "GitHub App을 연결할 팀을 찾을 수 없습니다."
                ));
    }
}
