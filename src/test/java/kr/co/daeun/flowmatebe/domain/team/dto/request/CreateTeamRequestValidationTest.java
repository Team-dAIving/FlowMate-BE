package kr.co.daeun.flowmatebe.domain.team.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTeamRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void themeColor는_6자리_HEX_색상_코드여야_한다() {
        CreateTeamRequest validRequest = request("#aB12Ef");
        CreateTeamRequest invalidRequest = request("RED");

        assertThat(validator.validate(validRequest)).isEmpty();
        assertThat(validator.validate(invalidRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("themeColor");
    }

    @Test
    void 팀원_목록은_비어_있어도_된다() {
        assertThat(validator.validate(request("#FFFFFF"))).isEmpty();
    }

    private CreateTeamRequest request(String themeColor) {
        return new CreateTeamRequest("FlowMate", null, themeColor, List.of());
    }
}
