package kr.co.daeun.flowmatebe.global.auth.oauth;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OAuth2LoginFailureHandlerTest {

    @Test
    void 데스크톱_실패_콜백에도_state를_전달한다() throws Exception {
        AuthProperties authProperties = new AuthProperties(
                "test-jwt-secret-must-be-at-least-32-bytes-long",
                "flowmate",
                Duration.ofHours(1),
                "https://flow-mate-fe.vercel.app",
                "https://flow-mate-fe.vercel.app/oauth/callback",
                "flowmate://oauth/callback",
                Duration.ofMinutes(2)
        );
        OAuthClientContext clientContext = new OAuthClientContext();
        OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(
                authProperties,
                clientContext
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        clientContext.save(request, OAuthClient.DESKTOP, "frontend-state");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                mock(AuthenticationException.class)
        );

        assertThat(response.getRedirectedUrl()).isEqualTo(
                "flowmate://oauth/callback?error=oauth_login_failed&state=frontend-state"
        );
        assertThat(request.getSession(false)).isNull();
    }
}
