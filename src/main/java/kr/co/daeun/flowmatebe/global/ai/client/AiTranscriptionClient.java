package kr.co.daeun.flowmatebe.global.ai.client;

import kr.co.daeun.flowmatebe.global.ai.dto.AiTranscriptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Set;

@Component
@Slf4j
public class AiTranscriptionClient {

    private static final Set<String> VALID_STATUSES = Set.of(
            "QUEUED",
            "IN_PROGRESS",
            "COMPLETED",
            "FAILED"
    );

    private final RestClient restClient;

    public AiTranscriptionClient(
            @Qualifier("aiTranscriptionRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public AiTranscriptionResponse upload(
            Long meetingId,
            Path file,
            String filename,
            String contentType
    ) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("meetingId", meetingId.toString());
        body.part("file", new FileSystemResource(file))
                .filename(filename)
                .contentType(parseContentType(contentType));
        try {
            AiTranscriptionResponse response = restClient.post()
                    .uri("/ai/v1/transcriptions/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(AiTranscriptionResponse.class);
            return validate(response, meetingId);
        } catch (ResourceAccessException exception) {
            log.warn("AI STT 서버에 연결할 수 없습니다.", exception);
            throw unavailable(exception);
        } catch (RestClientResponseException exception) {
            log.warn("AI STT 업로드 호출이 실패했습니다. status={}", exception.getStatusCode());
            throw upstreamFailure(exception);
        }
    }

    public AiTranscriptionResponse get(String jobId, Long meetingId) {
        try {
            AiTranscriptionResponse response = restClient.get()
                    .uri("/ai/v1/transcriptions/{jobId}", jobId)
                    .retrieve()
                    .body(AiTranscriptionResponse.class);
            return validate(response, meetingId);
        } catch (ResourceAccessException exception) {
            log.warn("AI STT 서버에 연결할 수 없습니다. jobId={}", jobId, exception);
            throw unavailable(exception);
        } catch (RestClientResponseException exception) {
            log.warn(
                    "AI STT 결과 조회가 실패했습니다. jobId={}, status={}",
                    jobId,
                    exception.getStatusCode()
            );
            throw upstreamFailure(exception);
        }
    }

    private AiTranscriptionResponse validate(
            AiTranscriptionResponse response,
            Long expectedMeetingId
    ) {
        if (response == null
                || response.meetingId() == null
                || !response.meetingId().equals(expectedMeetingId)
                || response.jobId() == null
                || response.jobId().isBlank()
                || response.status() == null
                || !VALID_STATUSES.contains(response.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI STT 서버가 올바르지 않은 응답을 반환했습니다."
            );
        }
        if ("COMPLETED".equals(response.status()) && response.transcript() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI STT 서버가 transcript 없이 완료 응답을 반환했습니다."
            );
        }
        return response;
    }

    private MediaType parseContentType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private ResponseStatusException unavailable(Exception exception) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI STT 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                exception
        );
    }

    private ResponseStatusException upstreamFailure(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI STT 서버가 아직 준비되지 않았습니다.",
                    exception
            );
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI 음성 변환 요청에 실패했습니다.",
                exception
        );
    }
}
