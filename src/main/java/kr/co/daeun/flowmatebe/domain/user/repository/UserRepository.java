package kr.co.daeun.flowmatebe.domain.user.repository;

import kr.co.daeun.flowmatebe.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserCodeIgnoreCase(String userCode);

    Optional<User> findByGithubUserId(Long githubUserId);
}
