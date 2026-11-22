package kr.co.daeun.flowmatebe.global.auth.oauth;

import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;

import java.util.Arrays;
import java.util.Optional;

public enum OAuthClient {
    DESKTOP("desktop"),
    WEB("web");

    private final String value;

    OAuthClient(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String redirectUri(AuthProperties authProperties) {
        return this == DESKTOP
                ? authProperties.desktopRedirectUri()
                : authProperties.frontendRedirectUri();
    }

    public static Optional<OAuthClient> from(String value) {
        return Arrays.stream(values())
                .filter(client -> client.value.equals(value))
                .findFirst();
    }
}
