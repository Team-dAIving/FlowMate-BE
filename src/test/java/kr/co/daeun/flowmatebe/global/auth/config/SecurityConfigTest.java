package kr.co.daeun.flowmatebe.global.auth.config;

import kr.co.daeun.flowmatebe.global.auth.oauth.FlowMateOAuth2UserService;
import kr.co.daeun.flowmatebe.global.auth.oauth.OAuth2LoginFailureHandler;
import kr.co.daeun.flowmatebe.global.auth.oauth.OAuth2LoginSuccessHandler;
import kr.co.daeun.flowmatebe.global.auth.oauth.OAuthClientContext;
import kr.co.daeun.flowmatebe.global.error.ApiErrorResponseWriter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.cors.CorsConfiguration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void 개발_서버와_Electron_file_origin을_허용한다() {
        AuthProperties authProperties = new AuthProperties(
                "test-jwt-secret-must-be-at-least-32-bytes-long",
                "flowmate",
                Duration.ofHours(1),
                "http://localhost:5173",
                "http://localhost:5173/oauth/callback",
                "flowmate://oauth/callback",
                Duration.ofMinutes(2)
        );
        SecurityConfig securityConfig = new SecurityConfig(
                authProperties,
                mock(FlowMateOAuth2UserService.class),
                mock(OAuth2LoginSuccessHandler.class),
                mock(OAuth2LoginFailureHandler.class),
                mock(JwtDecoder.class),
                mock(ApiErrorResponseWriter.class),
                new OAuthClientContext()
        );
        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .contains(
                        "http://localhost:5173",
                        "https://flow-mate-fe.vercel.app",
                        "null"
                );
        assertThat(configuration.getAllowedHeaders())
                .contains("Authorization", "Content-Type");
        assertThat(configuration.checkOrigin("file://")).isEqualTo("file://");
    }
}
