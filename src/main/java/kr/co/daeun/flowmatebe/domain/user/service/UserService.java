package kr.co.daeun.flowmatebe.domain.user.service;

import kr.co.daeun.flowmatebe.domain.user.dto.response.UserCodeLookupResponse;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Pattern USER_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6}$");

    private final UserRepository userRepository;

    public UserCodeLookupResponse getUserByCode(String userCode) {
        String normalizedCode = normalizeAndValidateUserCode(userCode);
        User user = userRepository.findByUserCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 유저코드를 가진 사용자가 없습니다."
                ));

        return UserCodeLookupResponse.from(user);
    }

    private String normalizeAndValidateUserCode(String userCode) {
        String normalizedCode = userCode.strip().toUpperCase(Locale.ROOT);
        if (!USER_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "유저코드는 영문 대문자와 숫자로 구성된 6자리여야 합니다."
            );
        }
        return normalizedCode;
    }
}
