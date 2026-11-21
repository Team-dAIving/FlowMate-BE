package kr.co.daeun.flowmatebe.global.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(AuthProperties authProperties) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey(authProperties))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(AuthProperties authProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey(authProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(
                authProperties.jwtIssuer()
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private SecretKey jwtSecretKey(AuthProperties authProperties) {
        return new SecretKeySpec(
                authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }
}
