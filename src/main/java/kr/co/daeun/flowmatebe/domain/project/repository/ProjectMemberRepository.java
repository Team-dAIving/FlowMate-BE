package kr.co.daeun.flowmatebe.domain.project.repository;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

    @Query("""
            select pm
            from ProjectMember pm
            join fetch pm.user
            where pm.project.id in :projectIds
            order by pm.project.id, pm.id
            """)
    List<ProjectMember> findAllWithUserByProjectIds(
            @Param("projectIds") Collection<Long> projectIds
    );

    @Query("""
            select pm
            from ProjectMember pm
            join fetch pm.user
            where pm.project.id = :projectId
            order by pm.id
            """)
    List<ProjectMember> findAllWithUserByProjectId(@Param("projectId") Long projectId);
}
