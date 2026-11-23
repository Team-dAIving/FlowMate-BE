package kr.co.daeun.flowmatebe.domain.project.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateProjectRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void PM과_디자인_파트를_허용한다() {
        assertThat(validator.validate(request("pm", "ai"))).isEmpty();
        assertThat(validator.validate(request("design", "ai"))).isEmpty();
    }

    @Test
    void AI_저장소_역할을_허용한다() {
        assertThat(validator.validate(request("backend", "ai"))).isEmpty();
    }

    @Test
    void 알_수_없는_파트와_저장소_역할은_거부한다() {
        assertThat(validator.validate(request("planner", "mobile")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("members[0].part", "repositories[0].role");
    }

    private CreateProjectRequest request(String part, String repositoryRole) {
        return new CreateProjectRequest(
                "FlowMate",
                "설명",
                "최종 목표",
                "프로젝트 상세 설계 내용",
                "web",
                LocalDate.of(2026, 11, 23),
                LocalDate.of(2026, 12, 23),
                10L,
                1L,
                List.of(new CreateProjectRequest.Member(
                        1L,
                        part,
                        List.of("Figma")
                )),
                List.of(),
                List.of(new CreateProjectRequest.Repository(
                        1001L,
                        repositoryRole
                ))
        );
    }
}
