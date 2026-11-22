package kr.co.daeun.flowmatebe.domain.project.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateIssueDraftsRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 파트는_약속된_소문자_값만_허용한다() {
        GenerateIssueDraftsRequest request = new GenerateIssueDraftsRequest(
                "FlowMate",
                "설명",
                "목표",
                "WEB",
                LocalDate.of(2026, 11, 22),
                LocalDate.of(2026, 12, 31),
                null,
                1L,
                List.of(new GenerateIssueDraftsRequest.Member(
                        1L,
                        "BACKEND",
                        "Spring Boot, Java",
                        null
                ))
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("members[0].part");
    }
}
