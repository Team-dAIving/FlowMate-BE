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
public class MeetingAnalysisPoller {

    private final MeetingRepository meetingRepository;
    private final MeetingAnalysisService analysisService;

    @Scheduled(fixedDelayString = "${app.meeting.analysis-poll-interval:PT5S}")
    public void poll() {
        List<Long> meetingIds = meetingRepository
                .findTop20ByProcessingStatusInOrderByIdAsc(List.of(
                        MeetingProcessingStatus.STT_COMPLETED.name()
                ))
                .stream()
                .map(Meeting::getId)
                .toList();
        for (Long meetingId : meetingIds) {
            try {
                analysisService.analyze(meetingId);
            } catch (RuntimeException exception) {
                log.warn("회의 분석 실행 중 예기치 않은 오류가 발생했습니다. meetingId={}",
                        meetingId, exception);
            }
        }
    }
}
