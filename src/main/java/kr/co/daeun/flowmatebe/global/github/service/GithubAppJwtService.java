package kr.co.daeun.flowmatebe.global.github.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class GithubAppJwtService {

    private final GithubAppProperties properties;

    public String createAppJwt() {
        if (properties.appId() == null || !StringUtils.hasText(properties.privateKeyBase64())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub App 인증 정보가 설정되지 않았습니다."
            );
        }

        try {
            String pem = new String(
                    Base64.getDecoder().decode(properties.privateKeyBase64()),
                    StandardCharsets.UTF_8
            );
            JWK parsedKey = JWK.parseFromPEMEncodedObjects(pem);
            if (!(parsedKey instanceof RSAKey rsaKey) || !rsaKey.isPrivate()) {
                throw new IllegalArgumentException("RSA private key가 아닙니다.");
            }

            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.appId().toString())
                    .issueTime(Date.from(now.minusSeconds(60)))
                    .expirationTime(Date.from(now.plusSeconds(9 * 60)))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    claims
            );
            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        } catch (IllegalArgumentException | JOSEException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub App private key를 읽을 수 없습니다."
            );
        }
    }
}
