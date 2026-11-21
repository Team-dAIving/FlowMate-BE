package kr.co.daeun.flowmatebe.global.auth.dto;

import kr.co.daeun.flowmatebe.domain.user.entity.User;
import org.springframework.util.StringUtils;

public record AuthUserResponse(
        Long userId,
        String userCode,
        String nickname,
        String email,
        String githubLogin,
        String avatarUrl,
        boolean onboardingRequired
) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUserCode(),
                user.getNickname(),
                user.getEmail(),
                user.getGithubLogin(),
                user.getAvatarUrl(),
                !StringUtils.hasText(user.getNickname())
        );
    }
}
