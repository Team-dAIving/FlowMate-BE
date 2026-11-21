package kr.co.daeun.flowmatebe.global.auth.jwt;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    public String createAccessToken(Long userId) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.jwtIssuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(authProperties.accessTokenTtl()))
                .claim("token_type", "access")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
