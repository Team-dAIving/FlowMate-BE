package kr.co.daeun.flowmatebe.global.storage.service;

import kr.co.daeun.flowmatebe.global.storage.config.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MeetingRecordingStorage {

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "audio/webm", "webm",
            "video/webm", "webm",
            "audio/mp4", "m4a",
            "video/mp4", "mp4",
            "audio/mpeg", "mp3",
            "audio/wav", "wav",
            "audio/x-wav", "wav",
            "audio/ogg", "ogg"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public UploadTarget createUploadTarget(Long meetingId, String rawContentType) {
        String contentType = normalizeContentType(rawContentType);
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String objectKey = "%s/%d/%s.%s".formatted(
                properties.recordingPrefix(),
                meetingId,
                UUID.randomUUID(),
                extension
        );
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlTtl())
                .putObjectRequest(putObjectRequest)
                .build();
        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            Map<String, String> headers = new LinkedHashMap<>();
            presigned.signedHeaders().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("host") && !values.isEmpty()) {
                    headers.put(name, values.get(0));
                }
            });
            return new UploadTarget(
                    objectKey,
                    presigned.url().toString(),
                    Map.copyOf(headers),
                    Instant.now().plus(properties.uploadUrlTtl())
            );
        } catch (SdkException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "녹음 업로드 URL을 생성할 수 없습니다.",
                    exception
            );
        }
    }

    public DownloadedRecording download(String objectKey) {
        HeadObjectResponse metadata;
        try {
            metadata = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (NoSuchKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "S3에서 업로드된 녹음 파일을 찾을 수 없습니다.",
                    exception
            );
        } catch (SdkException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3 녹음 파일을 확인할 수 없습니다.",
                    exception
            );
        }

        Path directory = null;
        try {
            directory = Files.createTempDirectory("flowmate-meeting-");
            String filename = objectKey.substring(objectKey.lastIndexOf('/') + 1);
            Path file = directory.resolve(filename);
            s3Client.getObject(GetObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toFile(file));
            String contentType = metadata.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new DownloadedRecording(directory, file, filename, contentType);
        } catch (IOException | SdkException exception) {
            deleteQuietly(directory);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3 녹음 파일을 내려받을 수 없습니다.",
                    exception
            );
        }
    }

    private String normalizeContentType(String rawContentType) {
        try {
            MediaType parsed = MediaType.parseMediaType(rawContentType);
            String normalized = (parsed.getType() + "/" + parsed.getSubtype())
                    .toLowerCase(Locale.ROOT);
            if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(normalized)) {
                throw unsupportedContentType();
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw unsupportedContentType();
        }
    }

    private ResponseStatusException unsupportedContentType() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 녹음 파일 형식입니다."
        );
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary files are best-effort cleanup targets.
        }
    }

    public record UploadTarget(
            String objectKey,
            String uploadUrl,
            Map<String, String> headers,
            Instant expiresAt
    ) {
    }

    public record DownloadedRecording(
            Path directory,
            Path file,
            String filename,
            String contentType
    ) implements AutoCloseable {

        @Override
        public void close() {
            deleteQuietly(file);
            deleteQuietly(directory);
        }
    }
}
