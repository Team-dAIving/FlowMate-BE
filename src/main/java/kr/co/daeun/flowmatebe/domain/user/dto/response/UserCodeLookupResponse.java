package kr.co.daeun.flowmatebe.domain.user.dto.response;

import kr.co.daeun.flowmatebe.domain.user.entity.User;

public record UserCodeLookupResponse(
        Long userId,
        String userCode,
        String nickname,
        String avatarUrl
) {

    public static UserCodeLookupResponse from(User user) {
        return new UserCodeLookupResponse(
                user.getId(),
                user.getUserCode(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }
}
