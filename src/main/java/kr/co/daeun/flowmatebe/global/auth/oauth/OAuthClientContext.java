package kr.co.daeun.flowmatebe.global.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OAuthClientContext {

    static final String CLIENT_ATTRIBUTE = OAuthClientContext.class.getName() + ".client";
    static final String STATE_ATTRIBUTE = OAuthClientContext.class.getName() + ".state";

    public void save(HttpServletRequest request, OAuthClient client, String state) {
        HttpSession session = request.getSession(true);
        session.setAttribute(CLIENT_ATTRIBUTE, client);
        session.setAttribute(STATE_ATTRIBUTE, state);
    }

    public Optional<Value> load(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object client = session.getAttribute(CLIENT_ATTRIBUTE);
        Object state = session.getAttribute(STATE_ATTRIBUTE);
        if (!(client instanceof OAuthClient oauthClient) || !(state instanceof String frontendState)) {
            return Optional.empty();
        }
        return Optional.of(new Value(oauthClient, frontendState));
    }

    public record Value(OAuthClient client, String state) {
    }
}
