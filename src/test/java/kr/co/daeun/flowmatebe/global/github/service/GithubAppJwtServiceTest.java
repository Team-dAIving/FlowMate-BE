package kr.co.daeun.flowmatebe.global.github.service;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class GithubAppJwtServiceTest {

    @Test
    void App_ID와_private_key로_RS256_JWT를_발급한다() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String encodedPem = Base64.getEncoder().encodeToString(pem.getBytes());
        GithubAppJwtService service = new GithubAppJwtService(new GithubAppProperties(
                "flowmate-integration",
                5_876_834L,
                encodedPem,
                null,
                Duration.ofMinutes(10)
        ));

        SignedJWT jwt = SignedJWT.parse(service.createAppJwt());

        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("5876834");
        assertThat(jwt.verify(new RSASSAVerifier((RSAPublicKey) keyPair.getPublic()))).isTrue();
    }
}
