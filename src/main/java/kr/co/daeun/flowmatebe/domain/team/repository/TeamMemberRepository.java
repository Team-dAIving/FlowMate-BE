package kr.co.daeun.flowmatebe.domain.team.repository;

import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("""
            select tm
            from TeamMember tm
            join fetch tm.user
            where tm.team.id in :teamIds
            order by tm.id
            """)
    List<TeamMember> findAllWithUserByTeamIds(@Param("teamIds") Collection<Long> teamIds);
}
