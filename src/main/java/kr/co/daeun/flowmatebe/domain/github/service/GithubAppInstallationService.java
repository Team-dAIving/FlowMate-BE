package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.dto.response.GithubAppInstallResponse;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public GithubAppInstallResponse createInstallUrl(Long currentUserId) {
        String slug = githubAppProperties.slug();
        if (!StringUtils.hasText(slug)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub App이 설정되지 않았습니다."
            );
        }

        GithubInstallationStateService.InstallationState state = stateService.create(currentUserId);
        String installUrl = UriComponentsBuilder.fromUriString(INSTALLATION_URL)
                .queryParam("state", state.value())
                .buildAndExpand(slug.strip())
                .encode()
                .toUriString();

        return new GithubAppInstallResponse(installUrl, state.expiresAt());
    }
}
