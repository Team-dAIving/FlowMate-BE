package kr.co.daeun.flowmatebe.global.auth.jwt;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    @Test
    void 로컬_사용자_ID를_subject로_발급한다() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        AtomicReference<JwtEncoderParameters> capturedParameters = new AtomicReference<>();
        when(encoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            capturedParameters.set(parameters);
            return new Jwt(
                    "encoded-token",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "HS256"),
                    parameters.getClaims().getClaims()
            );
        });
        AuthProperties properties = new AuthProperties(
                "test-jwt-secret-must-be-at-least-32-bytes-long",
                "flowmate",
                Duration.ofHours(1),
                "http://localhost:5173",
                "http://localhost:5173/oauth/callback",
                "flowmate://oauth/callback",
                Duration.ofMinutes(2)
        );
        JwtTokenService service = new JwtTokenService(encoder, properties);

        String token = service.createAccessToken(42L);

        assertThat(token).isEqualTo("encoded-token");
        assertThat(capturedParameters.get().getClaims().getSubject()).isEqualTo("42");
        assertThat(capturedParameters.get().getClaims().getClaimAsString("token_type"))
                .isEqualTo("access");
    }
}
