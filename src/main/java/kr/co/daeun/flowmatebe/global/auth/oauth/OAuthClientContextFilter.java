package kr.co.daeun.flowmatebe.global.auth.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.daeun.flowmatebe.global.error.ApiErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class OAuthClientContextFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_PATH = "/oauth2/authorization/github";
    private static final int MAX_STATE_LENGTH = 128;

    private final OAuthClientContext clientContext;
    private final ApiErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!HttpMethod.GET.matches(request.getMethod())
                || !AUTHORIZATION_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        OAuthClient client = OAuthClient.from(request.getParameter("client")).orElse(null);
        String state = request.getParameter("state");
        if (client == null) {
            errorResponseWriter.write(
                    response,
                    HttpStatus.BAD_REQUEST,
                    "client는 desktop 또는 web이어야 합니다."
            );
            return;
        }
        if (state == null || state.isBlank() || state.length() > MAX_STATE_LENGTH) {
            errorResponseWriter.write(
                    response,
                    HttpStatus.BAD_REQUEST,
                    "state는 1자 이상 128자 이하여야 합니다."
            );
            return;
        }

        clientContext.save(request, client, state);
        filterChain.doFilter(request, response);
    }
}
