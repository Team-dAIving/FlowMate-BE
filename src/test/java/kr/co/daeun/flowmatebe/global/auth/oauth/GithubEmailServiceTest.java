package kr.co.daeun.flowmatebe.global.auth.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubEmailServiceTest {

    private MockRestServiceServer server;
    private GithubEmailService githubEmailService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder
                .baseUrl("https://api.github.com")
                .build();
        githubEmailService = new GithubEmailService(restClient);
    }

    @Test
    void 인증된_기본_이메일을_선택한다() {
        server.expect(once(), requestTo("https://api.github.com/user/emails"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer github-token"))
                .andRespond(withSuccess("""
                        [
                          {"email":"secondary@example.com","primary":false,"verified":true},
                          {"email":"primary@example.com","primary":true,"verified":true}
                        ]
                        """, MediaType.APPLICATION_JSON));

        Optional<String> email = githubEmailService.findPrimaryVerifiedEmail("github-token");

        assertThat(email).contains("primary@example.com");
        server.verify();
    }

    @Test
    void 기본_이메일이_인증되지_않았으면_사용하지_않는다() {
        server.expect(once(), requestTo("https://api.github.com/user/emails"))
                .andRespond(withSuccess("""
                        [{"email":"primary@example.com","primary":true,"verified":false}]
                        """, MediaType.APPLICATION_JSON));

        Optional<String> email = githubEmailService.findPrimaryVerifiedEmail("github-token");

        assertThat(email).isEmpty();
        server.verify();
    }
}
