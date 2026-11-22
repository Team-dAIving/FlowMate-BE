package kr.co.daeun.flowmatebe.global.auth.service;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.auth.dto.TokenExchangeResponse;
import kr.co.daeun.flowmatebe.global.auth.jwt.JwtTokenService;
import kr.co.daeun.flowmatebe.global.auth.oauth.OAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OAuthCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;
    private final Map<String, PendingToken> pendingTokens = new ConcurrentHashMap<>();

    public String create(Long userId, boolean onboardingRequired, OAuthClient client) {
        Instant now = Instant.now();
        pendingTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendingTokens.put(
                code,
                new PendingToken(
                        userId,
                        onboardingRequired,
                        client,
                        now.plus(authProperties.oauthCodeTtl())
                )
        );
        return code;
    }

    public TokenExchangeResponse exchange(String code, OAuthClient client) {
        PendingToken pendingToken = pendingTokens.remove(code);
        if (pendingToken == null
                || pendingToken.expiresAt().isBefore(Instant.now())
                || pendingToken.client() != client) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인증 code가 유효하지 않거나 만료되었습니다."
            );
        }

        return new TokenExchangeResponse(
                jwtTokenService.createAccessToken(pendingToken.userId()),
                pendingToken.onboardingRequired()
        );
    }

    private record PendingToken(
            Long userId,
            boolean onboardingRequired,
            OAuthClient client,
            Instant expiresAt
    ) {
    }
}
