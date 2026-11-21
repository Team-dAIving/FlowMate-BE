package kr.co.daeun.flowmatebe.global.auth.service;

import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import kr.co.daeun.flowmatebe.global.auth.dto.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    public AuthUserResponse getCurrentUser(Long userId) {
        return AuthUserResponse.from(findUser(userId));
    }

    @Transactional
    public AuthUserResponse updateNickname(Long userId, String nickname) {
        User user = findUser(userId);
        user.setNickname(nickname.strip());
        return AuthUserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "인증된 사용자를 찾을 수 없습니다."
                ));
    }
}
