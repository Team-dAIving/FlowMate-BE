package kr.co.daeun.flowmatebe.domain.project.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import kr.co.daeun.flowmatebe.domain.user.entity.User;

import java.util.List;

@Entity
@Table(
        name = "project_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_members_project_user",
                columnNames = {"project_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text")
    private String role;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "parts")
    private String[] parts;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "tech_stack", nullable = false)
    private String[] techStack = new String[0];

    public List<String> getParts() {
        return parts == null ? null : List.of(parts);
    }

    public void setParts(List<String> parts) {
        this.parts = parts == null ? null : parts.toArray(String[]::new);
    }

    public List<String> getTechStack() {
        return List.of(techStack);
    }

    public void setTechStack(List<String> techStack) {
        this.techStack = techStack == null
                ? new String[0]
                : techStack.toArray(String[]::new);
    }
}
