package kr.co.daeun.flowmatebe.global.ai.client;

import kr.co.daeun.flowmatebe.global.ai.dto.AiMeetingAnalyzeRequest;
import kr.co.daeun.flowmatebe.global.ai.dto.AiMeetingAnalyzeResponse;
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
public class AiMeetingAnalysisClient {

    private final RestClient restClient;

    public AiMeetingAnalysisClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AiMeetingAnalyzeResponse analyze(AiMeetingAnalyzeRequest request) {
        try {
            AiMeetingAnalyzeResponse response = restClient.post()
                    .uri("/ai/v1/meetings/analyze")
                    .body(request)
                    .retrieve()
                    .body(AiMeetingAnalyzeResponse.class);
            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 회의 분석 서버가 빈 응답을 반환했습니다."
                );
            }
            return response;
        } catch (ResourceAccessException exception) {
            log.warn("AI 회의 분석 서버에 연결할 수 없습니다. meetingId={}",
                    request.meetingId(), exception);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 회의 분석 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        } catch (RestClientResponseException exception) {
            log.warn("AI 회의 분석 호출이 실패했습니다. meetingId={}, status={}",
                    request.meetingId(), exception.getStatusCode());
            if (exception.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI 회의 분석 서버가 아직 준비되지 않았습니다.",
                        exception
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 회의 분석 요청에 실패했습니다.",
                    exception
            );
        }
    }
}
