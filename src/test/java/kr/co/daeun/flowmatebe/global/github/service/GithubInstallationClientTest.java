package kr.co.daeun.flowmatebe.global.github.service;

import kr.co.daeun.flowmatebe.global.github.dto.GithubCreatedIssue;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubInstallationClientTest {

    private MockRestServiceServer server;
    private GithubInstallationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.test");
        server = MockRestServiceServer.bindTo(builder).build();
        GithubAppJwtService jwtService = mock(GithubAppJwtService.class);
        when(jwtService.createAppJwt()).thenReturn("app-jwt");
        client = new GithubInstallationClient(builder.build(), jwtService);
    }

    @Test
    void 설치_토큰으로_GitHub_이슈를_생성한다() {
        server.expect(once(), requestTo(
                        "https://api.github.test/app/installations/500/access_tokens"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer app-jwt"))
                .andRespond(withSuccess(
                        "{\"token\":\"installation-token\"}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(once(), requestTo(
                        "https://api.github.test/repos/Team-dAIving/FlowMate-BE/issues"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer installation-token"))
                .andExpect(content().json("""
                        {
                          "title": "로그인 API 구현",
                          "body": "JWT 로그인을 구현합니다.",
                          "labels": ["feature", "backend"]
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "number": 31,
                          "html_url": "https://github.com/Team-dAIving/FlowMate-BE/issues/31"
                        }
                        """, MediaType.APPLICATION_JSON));

        GithubCreatedIssue issue = client.createIssue(
                500L,
                "Team-dAIving",
                "FlowMate-BE",
                "로그인 API 구현",
                "JWT 로그인을 구현합니다.",
                List.of("feature", "backend")
        );

        assertThat(issue.number()).isEqualTo(31);
        assertThat(issue.htmlUrl())
                .isEqualTo("https://github.com/Team-dAIving/FlowMate-BE/issues/31");
        server.verify();
    }

    @Test
    void App_JWT로_installation의_Organization을_조회한다() {
        server.expect(once(), requestTo(
                        "https://api.github.test/app/installations/500"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer app-jwt"))
                .andRespond(withSuccess("""
                        {
                          "id": 500,
                          "account": {
                            "id": 900,
                            "login": "Team-dAIving",
                            "type": "Organization"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        GithubInstallationMetadata installation = client.getInstallation(500L);

        assertThat(installation.accountId()).isEqualTo(900L);
        assertThat(installation.accountLogin()).isEqualTo("Team-dAIving");
        assertThat(installation.accountType()).isEqualTo("Organization");
        server.verify();
    }
}
