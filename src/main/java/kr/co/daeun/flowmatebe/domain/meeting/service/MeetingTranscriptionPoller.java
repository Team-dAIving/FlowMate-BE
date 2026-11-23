package kr.co.daeun.flowmatebe.domain.meeting.service;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingProcessingStatus;
import kr.co.daeun.flowmatebe.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingTranscriptionPoller {

    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;

    @Scheduled(fixedDelayString = "${app.meeting.stt-poll-interval:PT5S}")
    public void poll() {
        List<Long> meetingIds = meetingRepository
                .findTop20ByProcessingStatusInOrderByIdAsc(List.of(
                        MeetingProcessingStatus.STT_QUEUED.name(),
                        MeetingProcessingStatus.STT_IN_PROGRESS.name()
                ))
                .stream()
                .map(Meeting::getId)
                .toList();
        for (Long meetingId : meetingIds) {
            try {
                meetingService.refreshTranscription(meetingId);
            } catch (RuntimeException exception) {
                log.warn("STT 결과 조회를 다음 주기에 재시도합니다. meetingId={}", meetingId, exception);
            }
        }
    }
}
