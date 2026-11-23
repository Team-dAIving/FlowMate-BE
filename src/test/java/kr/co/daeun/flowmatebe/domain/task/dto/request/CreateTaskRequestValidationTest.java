package kr.co.daeun.flowmatebe.domain.task.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTaskRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void GitHub_라벨을_생략할_수_있다() {
        CreateTaskRequest request = validRequest();

        assertThat(request.labels()).isEmpty();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 제목_우선순위_일정과_GitHub_생성여부는_필수다() {
        CreateTaskRequest request = new CreateTaskRequest(
                " ",
                "설명",
                "NORMAL",
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("title", "priority", "dueDate", "createGithubIssue");
    }

    private CreateTaskRequest validRequest() {
        return new CreateTaskRequest(
                "로그인 API 구현",
                "JWT 로그인을 구현합니다.",
                "HIGH",
                1L,
                LocalDate.of(2026, 11, 30),
                8.0,
                null,
                false,
                null
        );
    }
}
