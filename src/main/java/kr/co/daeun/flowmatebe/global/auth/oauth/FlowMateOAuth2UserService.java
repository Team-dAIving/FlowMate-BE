package kr.co.daeun.flowmatebe.global.auth.oauth;

import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FlowMateOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final GithubEmailService githubEmailService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        Long githubUserId = requiredLong(oauth2User, "id");

        User user = userRepository.findByGithubUserId(githubUserId)
                .orElseGet(User::new);
        user.setGithubUserId(githubUserId);
        user.setGithubLogin(oauth2User.getAttribute("login"));
        user.setAvatarUrl(oauth2User.getAttribute("avatar_url"));

        String publicProfileEmail = oauth2User.getAttribute("email");
        String email = githubEmailService.findPrimaryVerifiedEmail(
                        userRequest.getAccessToken().getTokenValue()
                )
                .orElse(publicProfileEmail);
        if (StringUtils.hasText(email)) {
            user.setEmail(email);
        }

        User savedUser = userRepository.saveAndFlush(user);
        boolean onboardingRequired = !StringUtils.hasText(savedUser.getNickname());
        return new FlowMateOAuth2User(oauth2User, savedUser.getId(), onboardingRequired);
    }

    private Long requiredLong(OAuth2User oauth2User, String attributeName) {
        Object value = oauth2User.getAttributes().get(attributeName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                // 아래 공통 예외로 처리한다.
            }
        }
        throw new OAuth2AuthenticationException("GitHub 사용자 ID를 확인할 수 없습니다.");
    }
}
