package kr.co.daeun.flowmatebe.domain.project.repository;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);
}
