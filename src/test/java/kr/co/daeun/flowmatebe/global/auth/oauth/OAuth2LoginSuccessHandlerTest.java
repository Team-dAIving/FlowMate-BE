package kr.co.daeun.flowmatebe.global.auth.oauth;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.auth.service.OAuthCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    @Test
    void 웹_콜백에_일회용_code와_state를_전달한다() throws Exception {
        OAuthCodeService oauthCodeService = mock(OAuthCodeService.class);
        when(oauthCodeService.create(42L, true, OAuthClient.WEB)).thenReturn("one-time-code");

        AuthProperties authProperties = new AuthProperties(
                "test-jwt-secret-must-be-at-least-32-bytes-long",
                "flowmate",
                Duration.ofHours(1),
                "http://localhost:5173",
                "http://localhost:5173/oauth/callback",
                "flowmate://oauth/callback",
                Duration.ofMinutes(2)
        );
        OAuthClientContext clientContext = new OAuthClientContext();
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(
                oauthCodeService,
                clientContext,
                authProperties
        );

        OAuth2User delegate = mock(OAuth2User.class);
        FlowMateOAuth2User principal = new FlowMateOAuth2User(delegate, 42L, true);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest();
        clientContext.save(request, OAuthClient.WEB, "frontend-state");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(
                "http://localhost:5173/oauth/callback"
                        + "?code=one-time-code&state=frontend-state"
        );
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }
}
