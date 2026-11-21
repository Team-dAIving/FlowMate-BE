package kr.co.daeun.flowmatebe.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String nickname;

    @Column(columnDefinition = "text")
    private String email;

    @Column(name = "github_user_id", unique = true)
    private Long githubUserId;

    @Column(name = "github_login", columnDefinition = "text")
    private String githubLogin;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "user_code", nullable = false, unique = true, updatable = false)
    private String userCode;
}
