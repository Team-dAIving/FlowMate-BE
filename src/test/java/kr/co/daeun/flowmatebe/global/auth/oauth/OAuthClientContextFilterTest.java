package kr.co.daeun.flowmatebe.global.auth.oauth;

import jakarta.servlet.FilterChain;
import kr.co.daeun.flowmatebe.global.error.ApiErrorResponseWriter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OAuthClientContextFilterTest {

    private final OAuthClientContext clientContext = new OAuthClientContext();
    private final ApiErrorResponseWriter errorResponseWriter = mock(ApiErrorResponseWriter.class);
    private final OAuthClientContextFilter filter = new OAuthClientContextFilter(
            clientContext,
            errorResponseWriter
    );

    @Test
    void 허용된_client와_state를_세션에_저장한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest("web", "random-state");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(clientContext.load(request)).contains(
                new OAuthClientContext.Value(OAuthClient.WEB, "random-state")
        );
        verify(chain).doFilter(request, response);
    }

    @Test
    void 허용되지_않은_client를_거부한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest("https://evil.example", "state");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(errorResponseWriter).write(
                response,
                HttpStatus.BAD_REQUEST,
                "client는 desktop 또는 web이어야 합니다."
        );
        assertThat(clientContext.load(request)).isEmpty();
    }

    @Test
    void 너무_긴_state를_거부한다() throws Exception {
        MockHttpServletRequest request = authorizationRequest("web", "a".repeat(129));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(errorResponseWriter).write(
                response,
                HttpStatus.BAD_REQUEST,
                "state는 1자 이상 128자 이하여야 합니다."
        );
    }

    private MockHttpServletRequest authorizationRequest(String client, String state) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorization/github"
        );
        request.setParameter("client", client);
        request.setParameter("state", state);
        return request;
    }
}
