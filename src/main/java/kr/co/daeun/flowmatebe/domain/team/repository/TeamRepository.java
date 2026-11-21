package kr.co.daeun.flowmatebe.domain.team.repository;

import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("""
            select tm.team
            from TeamMember tm
            where tm.user.id = :userId
            order by tm.team.id
            """)
    List<Team> findAllByMemberUserIdOrderByIdAsc(@Param("userId") Long userId);
}
