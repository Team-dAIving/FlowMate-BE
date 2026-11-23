package kr.co.daeun.flowmatebe.domain.meeting.entity;

public enum MeetingProcessingStatus {
    RECORDING,
    UPLOAD_PENDING,
    STT_QUEUED,
    STT_IN_PROGRESS,
    STT_COMPLETED,
    ANALYZING,
    COMPLETED,
    FAILED
}
