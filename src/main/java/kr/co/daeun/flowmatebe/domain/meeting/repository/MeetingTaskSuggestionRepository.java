package kr.co.daeun.flowmatebe.domain.meeting.repository;

import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface MeetingTaskSuggestionRepository
        extends JpaRepository<MeetingTaskSuggestion, Long> {

    void deleteAllByMeeting_Id(Long meetingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select mts
            from MeetingTaskSuggestion mts
            join fetch mts.meeting m
            join fetch m.project p
            where mts.id = :suggestionId
              and m.id = :meetingId
              and exists (
                  select pm.id
                  from ProjectMember pm
                  where pm.project = p
                    and pm.user.id = :userId
              )
            """)
    Optional<MeetingTaskSuggestion> findAccessibleByIdForUpdate(
            @Param("suggestionId") Long suggestionId,
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @Query("""
            select mts.meeting.id as meetingId, count(mts.id) as suggestionCount
            from MeetingTaskSuggestion mts
            where mts.meeting.id in :meetingIds
              and upper(mts.reviewStatus) = upper(:reviewStatus)
            group by mts.meeting.id
            """)
    List<MeetingSuggestionCount> countByMeetingIdsAndReviewStatus(
            @Param("meetingIds") List<Long> meetingIds,
            @Param("reviewStatus") String reviewStatus
    );

    interface MeetingSuggestionCount {

        Long getMeetingId();

        long getSuggestionCount();
    }
}
