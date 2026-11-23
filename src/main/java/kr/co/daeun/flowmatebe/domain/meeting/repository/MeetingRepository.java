package kr.co.daeun.flowmatebe.domain.meeting.repository;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    long countByProject_Id(Long projectId);

    List<Meeting> findAllByProject_IdOrderByHeldAtDescIdDesc(Long projectId);

    @Query("""
            select m
            from Meeting m
            join fetch m.project p
            where m.id = :meetingId
              and exists (
                  select pm.id
                  from ProjectMember pm
                  where pm.project = p
                    and pm.user.id = :userId
              )
            """)
    Optional<Meeting> findAccessibleByIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    List<Meeting> findTop20ByProcessingStatusInOrderByIdAsc(
            Collection<String> processingStatuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Meeting m join fetch m.project where m.id = :meetingId")
    Optional<Meeting> findByIdForUpdate(@Param("meetingId") Long meetingId);
}
