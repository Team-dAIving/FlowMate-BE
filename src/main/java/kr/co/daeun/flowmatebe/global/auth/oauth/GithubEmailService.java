package kr.co.daeun.flowmatebe.global.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GithubEmailService {

    private static final ParameterizedTypeReference<List<GithubEmailResponse>> EMAIL_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public GithubEmailService(@Qualifier("githubRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<String> findPrimaryVerifiedEmail(String githubAccessToken) {
        try {
            List<GithubEmailResponse> emails = restClient.get()
                    .uri("/user/emails")
                    .headers(headers -> headers.setBearerAuth(githubAccessToken))
                    .retrieve()
                    .body(EMAIL_LIST_TYPE);

            if (emails == null) {
                return Optional.empty();
            }

            return emails.stream()
                    .filter(GithubEmailResponse::primary)
                    .filter(GithubEmailResponse::verified)
                    .map(GithubEmailResponse::email)
                    .filter(StringUtils::hasText)
                    .findFirst();
        } catch (RestClientException exception) {
            log.warn("GitHub 기본 이메일을 가져오지 못했습니다: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    record GithubEmailResponse(
            String email,
            boolean primary,
            boolean verified
    ) {
    }
}
