package kr.co.daeun.flowmatebe.global.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void 상태_예외를_code와_message_형식으로_응답한다() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(
                new ApiErrorResponse("NOT_FOUND", "사용자를 찾을 수 없습니다.")
        );
    }
}
