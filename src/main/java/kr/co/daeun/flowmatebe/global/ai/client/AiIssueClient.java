package kr.co.daeun.flowmatebe.global.ai.client;

import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateRequest;
import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class AiIssueClient {

    private final RestClient restClient;

    public AiIssueClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AiIssueGenerateResponse generateIssues(AiIssueGenerateRequest request) {
        try {
            AiIssueGenerateResponse response = restClient.post()
                    .uri("/ai/v1/projects/generate-issues")
                    .body(request)
                    .retrieve()
                    .body(AiIssueGenerateResponse.class);
            if (response == null || response.issues() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 서버가 올바르지 않은 응답을 반환했습니다."
                );
            }
            return response;
        } catch (ResourceAccessException exception) {
            log.warn("AI 서버에 연결할 수 없습니다.", exception);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
            );
        } catch (RestClientResponseException exception) {
            log.warn("AI 서버 호출이 실패했습니다. status={}", exception.getStatusCode());
            if (exception.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI 서버가 아직 준비되지 않았습니다."
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 업무 초안 생성에 실패했습니다."
            );
        }
    }
}
