package kr.co.daeun.flowmatebe.global.auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.daeun.flowmatebe.global.auth.config.AuthProperties;
import kr.co.daeun.flowmatebe.global.auth.service.OAuthCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthCodeService oauthCodeService;
    private final OAuthClientContext clientContext;
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        FlowMateOAuth2User principal = (FlowMateOAuth2User) authentication.getPrincipal();
        OAuthClientContext.Value context = clientContext.load(request)
                .orElseThrow(() -> new IllegalStateException("OAuth client context가 없습니다."));
        String code = oauthCodeService.create(
                principal.localUserId(),
                principal.onboardingRequired(),
                context.client()
        );

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        URI redirectUri = UriComponentsBuilder.fromUriString(context.client().redirectUri(authProperties))
                .queryParam("code", code)
                .queryParam("state", context.state())
                .build()
                .encode()
                .toUri();
        response.sendRedirect(redirectUri.toString());
    }
}
