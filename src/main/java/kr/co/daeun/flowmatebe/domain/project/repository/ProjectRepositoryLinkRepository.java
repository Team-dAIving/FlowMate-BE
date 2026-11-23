package kr.co.daeun.flowmatebe.domain.project.repository;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ProjectRepositoryLinkRepository extends JpaRepository<ProjectRepository, Long> {

    boolean existsByGithubRepositoryId(Long githubRepositoryId);

    Optional<ProjectRepository> findFirstByProject_IdAndRepositoryRoleIgnoreCaseOrderByIdAsc(
            Long projectId,
            String repositoryRole
    );

    List<ProjectRepository> findAllByProject_IdOrderByIdAsc(Long projectId);
}
