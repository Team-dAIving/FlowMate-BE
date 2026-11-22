package kr.co.daeun.flowmatebe.domain.task.dto.response;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.user.entity.User;

public record TaskAssigneeResponse(
        Long projectMemberId,
        Long userId,
        String nickname,
        String avatarUrl,
        String role
) {

    public static TaskAssigneeResponse from(ProjectMember projectMember) {
        if (projectMember == null) {
            return null;
        }
        User user = projectMember.getUser();
        return new TaskAssigneeResponse(
                projectMember.getId(),
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                projectMember.getRole()
        );
    }
}
