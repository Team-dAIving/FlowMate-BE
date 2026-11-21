package kr.co.daeun.flowmatebe.domain.user.controller;

import kr.co.daeun.flowmatebe.domain.user.dto.response.UserCodeLookupResponse;
import kr.co.daeun.flowmatebe.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/code/{userCode}")
    public ResponseEntity<UserCodeLookupResponse> getUserByCode(
            @PathVariable String userCode
    ) {
        return ResponseEntity.ok(userService.getUserByCode(userCode));
    }
}
