package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GithubInstallationStateService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String STATE_VERSION = "v2";
    private static final String SIGNING_CONTEXT = "github-installation-state:";

    private final AuthProperties authProperties;
    private final GithubAppProperties githubAppProperties;

    public InstallationState create(Long currentUserId, Long teamId) {
        Instant expiresAt = Instant.now().plus(githubAppProperties.installationStateTtl());
        String payload = String.join(
                ":",
                STATE_VERSION,
                currentUserId.toString(),
                teamId.toString(),
                Long.toString(expiresAt.getEpochSecond()),
                UUID.randomUUID().toString()
        );
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = encode(sign(payload));
        return new InstallationState(encodedPayload + "." + signature, expiresAt);
    }

    public ValidatedInstallationState validate(String state, Long currentUserId) {
        try {
            String[] tokenParts = state.split("\\.", -1);
            if (tokenParts.length != 2) {
                throw invalidState();
            }

            String payload = new String(
                    Base64.getUrlDecoder().decode(tokenParts[0]),
                    StandardCharsets.UTF_8
            );
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(tokenParts[1]);
            if (!MessageDigest.isEqual(sign(payload), suppliedSignature)) {
                throw invalidState();
            }

            String[] payloadParts = payload.split(":", 5);
            if (payloadParts.length != 5
                    || !STATE_VERSION.equals(payloadParts[0])
                    || !currentUserId.toString().equals(payloadParts[1])
                    || Instant.now().getEpochSecond() >= Long.parseLong(payloadParts[3])) {
                throw invalidState();
            }
            return new ValidatedInstallationState(Long.parseLong(payloadParts[2]));
        } catch (IllegalArgumentException exception) {
            throw invalidState();
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return mac.doFinal((SIGNING_CONTEXT + payload).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("GitHub App 설치 state에 서명할 수 없습니다.", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private ResponseStatusException invalidState() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "GitHub App 설치 요청이 유효하지 않거나 만료되었습니다."
        );
    }

    public record InstallationState(String value, Instant expiresAt) {
    }

    public record ValidatedInstallationState(Long teamId) {
    }
}
