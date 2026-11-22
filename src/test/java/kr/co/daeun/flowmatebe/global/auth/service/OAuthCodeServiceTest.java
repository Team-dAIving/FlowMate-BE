package kr.co.daeun.flowmatebe.global.auth.service;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.auth.dto.TokenExchangeResponse;
import kr.co.daeun.flowmatebe.global.auth.jwt.JwtTokenService;
import kr.co.daeun.flowmatebe.global.auth.oauth.OAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthCodeServiceTest {

    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final OAuthCodeService oauthCodeService = new OAuthCodeService(
            jwtTokenService,
            new AuthProperties(
                    "test-jwt-secret-must-be-at-least-32-bytes-long",
                    "flowmate",
                    Duration.ofHours(1),
                    "https://flow-mate-fe.vercel.app",
                    "https://flow-mate-fe.vercel.app/oauth/callback",
                    "flowmate://oauth/callback",
                    Duration.ofMinutes(2)
            )
    );

    @Test
    void code는_한_번만_토큰으로_교환할_수_있다() {
        when(jwtTokenService.createAccessToken(42L)).thenReturn("access-token");
        String code = oauthCodeService.create(42L, true, OAuthClient.WEB);

        TokenExchangeResponse response = oauthCodeService.exchange(code, OAuthClient.WEB);

        assertThat(response).isEqualTo(new TokenExchangeResponse("access-token", true));
        assertThatThrownBy(() -> oauthCodeService.exchange(code, OAuthClient.WEB))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 발급받은_client와_다르면_교환할_수_없다() {
        String code = oauthCodeService.create(42L, false, OAuthClient.DESKTOP);

        assertThatThrownBy(() -> oauthCodeService.exchange(code, OAuthClient.WEB))
                .isInstanceOf(ResponseStatusException.class);
    }
}
