package kr.co.daeun.flowmatebe.global.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
        String region,
        String bucket,
        String recordingPrefix,
        Duration uploadUrlTtl
) {

    public S3StorageProperties {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS_REGION은 필수입니다.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3_BUCKET은 필수입니다.");
        }
        if (recordingPrefix == null || recordingPrefix.isBlank()) {
            recordingPrefix = "flowmate/meetings";
        }
        recordingPrefix = recordingPrefix.strip();
        while (recordingPrefix.startsWith("/")) {
            recordingPrefix = recordingPrefix.substring(1);
        }
        while (recordingPrefix.endsWith("/")) {
            recordingPrefix = recordingPrefix.substring(0, recordingPrefix.length() - 1);
        }
        if (recordingPrefix.isBlank()) {
            throw new IllegalArgumentException("S3_RECORDING_PREFIX는 비어 있을 수 없습니다.");
        }
        if (uploadUrlTtl == null) {
            uploadUrlTtl = Duration.ofMinutes(15);
        }
        if (uploadUrlTtl.isNegative() || uploadUrlTtl.isZero()) {
            throw new IllegalArgumentException("S3_UPLOAD_URL_TTL은 0보다 커야 합니다.");
        }
    }
}
