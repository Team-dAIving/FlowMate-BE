package kr.co.daeun.flowmatebe.global.auth.controller;

import jakarta.validation.Valid;
import kr.co.daeun.flowmatebe.global.auth.dto.AuthUserResponse;
import kr.co.daeun.flowmatebe.global.auth.dto.TokenExchangeRequest;
import kr.co.daeun.flowmatebe.global.auth.dto.TokenExchangeResponse;
import kr.co.daeun.flowmatebe.global.auth.dto.UpdateNicknameRequest;
import kr.co.daeun.flowmatebe.global.auth.oauth.OAuthClient;
import kr.co.daeun.flowmatebe.global.auth.service.AuthService;
import kr.co.daeun.flowmatebe.global.auth.service.OAuthCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthCodeService oauthCodeService;

    @PostMapping("/token")
    public ResponseEntity<TokenExchangeResponse> exchangeToken(
            @Valid @RequestBody TokenExchangeRequest request
    ) {
        OAuthClient client = OAuthClient.from(request.client()).orElseThrow();
        return ResponseEntity.ok(oauthCodeService.exchange(request.code(), client));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(authService.getCurrentUser(userId(jwt)));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<AuthUserResponse> updateNickname(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return ResponseEntity.ok(authService.updateNickname(userId(jwt), request.nickname()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
