package kr.co.daeun.flowmatebe.domain.team.repository;

import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("""
            select tm.team
            from TeamMember tm
            where tm.user.id = :userId
            order by tm.team.id
            """)
    List<Team> findAllByMemberUserIdOrderByIdAsc(@Param("userId") Long userId);

    @Query("""
            select tm.team
            from TeamMember tm
            where tm.team.id = :teamId
              and tm.user.id = :userId
            """)
    Optional<Team> findByIdAndMemberUserId(
            @Param("teamId") Long teamId,
            @Param("userId") Long userId
    );
}
