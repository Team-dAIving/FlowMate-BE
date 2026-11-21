package kr.co.daeun.flowmatebe.global.auth.service;

import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import kr.co.daeun.flowmatebe.global.auth.dto.AuthUserResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthService authService = new AuthService(userRepository);

    @Test
    void nickname이_없으면_온보딩이_필요하다() {
        User user = user(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthUserResponse response = authService.getCurrentUser(1L);

        assertThat(response.onboardingRequired()).isTrue();
    }

    @Test
    void 닉네임을_설정하면_온보딩이_완료된다() {
        User user = user(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthUserResponse response = authService.updateNickname(1L, "  flowmate  ");

        assertThat(response.nickname()).isEqualTo("flowmate");
        assertThat(response.onboardingRequired()).isFalse();
        assertThat(user.getNickname()).isEqualTo("flowmate");
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }
}
