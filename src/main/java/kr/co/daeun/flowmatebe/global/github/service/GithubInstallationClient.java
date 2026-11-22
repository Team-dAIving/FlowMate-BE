package kr.co.daeun.flowmatebe.global.github.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.co.daeun.flowmatebe.global.github.dto.GithubIssueMetadata;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationRepositories;
import kr.co.daeun.flowmatebe.global.github.dto.GithubRepositoryMetadata;
import kr.co.daeun.flowmatebe.global.github.dto.GithubRepositorySnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class GithubInstallationClient {

    private final RestClient restClient;
    private final GithubAppJwtService appJwtService;

    public GithubInstallationClient(
            @Qualifier("githubRestClient") RestClient restClient,
            GithubAppJwtService appJwtService
    ) {
        this.restClient = restClient;
        this.appJwtService = appJwtService;
    }

    public GithubRepositoryMetadata getRepository(Long installationId, Long repositoryId) {
        try {
            String token = createInstallationToken(installationId);

            RepositoryResponse repository = restClient.get()
                    .uri("/repositories/{repositoryId}", repositoryId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(RepositoryResponse.class);
            if (repository == null || repository.owner() == null) {
                throw githubUnavailable();
            }
            return new GithubRepositoryMetadata(
                    repository.id(),
                    installationId,
                    repository.owner().login(),
                    repository.name(),
                    repository.htmlUrl()
            );
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 403
                    || exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "GitHub App이 접근할 수 없는 저장소입니다: " + repositoryId
                );
            }
            throw githubUnavailable();
        }
    }

    public GithubRepositorySnapshot getRepositorySnapshot(
            Long installationId,
            Long repositoryId
    ) {
        try {
            String token = createInstallationToken(installationId);
            RepositoryResponse repository = restClient.get()
                    .uri("/repositories/{repositoryId}", repositoryId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(RepositoryResponse.class);
            if (repository == null || repository.owner() == null) {
                throw githubUnavailable();
            }
            GithubRepositoryMetadata metadata = new GithubRepositoryMetadata(
                    repository.id(),
                    installationId,
                    repository.owner().login(),
                    repository.name(),
                    repository.htmlUrl()
            );
            try {
                return new GithubRepositorySnapshot(
                        metadata,
                        listIssues(token, metadata.ownerLogin(), metadata.repositoryName()),
                        true
                );
            } catch (RestClientResponseException exception) {
                return new GithubRepositorySnapshot(metadata, List.of(), false);
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 403
                    || exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "GitHub App이 접근할 수 없는 저장소입니다: " + repositoryId
                );
            }
            throw githubUnavailable();
        }
    }

    public GithubInstallationRepositories listRepositories(Long installationId) {
        try {
            String token = createInstallationToken(installationId);
            List<GithubInstallationRepositories.Repository> repositories = new ArrayList<>();
            for (int page = 1; page <= 10; page++) {
                int currentPage = page;
                InstallationRepositoriesResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/installation/repositories")
                                .queryParam("per_page", 100)
                                .queryParam("page", currentPage)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(InstallationRepositoriesResponse.class);
                if (response == null || response.repositories() == null
                        || response.repositories().isEmpty()) {
                    break;
                }
                response.repositories().stream()
                        .map(repository -> new GithubInstallationRepositories.Repository(
                                repository.id(),
                                repository.owner().login(),
                                repository.name(),
                                repository.fullName(),
                                repository.htmlUrl(),
                                repository.privateRepository()
                        ))
                        .forEach(repositories::add);
                if (response.repositories().size() < 100) {
                    break;
                }
            }
            return new GithubInstallationRepositories(
                    installationId,
                    List.copyOf(repositories)
            );
        } catch (RestClientResponseException exception) {
            throw githubUnavailable();
        }
    }

    private String createInstallationToken(Long installationId) {
        InstallationTokenResponse response = restClient.post()
                .uri("/app/installations/{installationId}/access_tokens", installationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appJwtService.createAppJwt())
                .retrieve()
                .body(InstallationTokenResponse.class);
        if (response == null || response.token() == null) {
            throw githubUnavailable();
        }
        return response.token();
    }

    private List<GithubIssueMetadata> listIssues(
            String token,
            String owner,
            String repositoryName
    ) {
        List<GithubIssueMetadata> issues = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            int currentPage = page;
            IssueResponse[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repository}/issues")
                            .queryParam("state", "all")
                            .queryParam("per_page", 100)
                            .queryParam("page", currentPage)
                            .build(owner, repositoryName))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(IssueResponse[].class);
            if (response == null || response.length == 0) {
                break;
            }
            for (IssueResponse issue : response) {
                if (issue.pullRequest() == null) {
                    issues.add(new GithubIssueMetadata(
                            issue.number(),
                            issue.title(),
                            issue.body(),
                            issue.state(),
                            issue.assignee() == null ? null : issue.assignee().login(),
                            issue.closedAt()
                    ));
                }
            }
            if (response.length < 100) {
                break;
            }
        }
        return List.copyOf(issues);
    }

    private ResponseStatusException githubUnavailable() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "GitHub API 응답을 확인할 수 없습니다."
        );
    }

    private record InstallationTokenResponse(String token) {
    }

    private record RepositoryResponse(
            Long id,
            String name,
            Owner owner,
            @JsonProperty("html_url") String htmlUrl
    ) {
    }

    private record Owner(String login) {
    }

    private record IssueResponse(
            Integer number,
            String title,
            String body,
            String state,
            Assignee assignee,
            @JsonProperty("closed_at") Instant closedAt,
            @JsonProperty("pull_request") Object pullRequest
    ) {
    }

    private record Assignee(String login) {
    }

    private record InstallationRepositoriesResponse(
            @JsonProperty("total_count") int totalCount,
            List<InstallationRepositoryResponse> repositories
    ) {
    }

    private record InstallationRepositoryResponse(
            Long id,
            String name,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("private") boolean privateRepository,
            Owner owner
    ) {
    }
}
