package kr.co.daeun.flowmatebe.domain.project.dto.response;

import java.util.List;

public record ProjectMembersResponse(
        long totalCount,
        List<Member> members
) {

    public ProjectMembersResponse(List<Member> members) {
        this(members.size(), members);
    }

    public record Member(
            Long projectMemberId,
            Long userId,
            String nickname,
            String email,
            String avatarUrl,
            List<String> parts,
            List<String> techStack,
            boolean leader,
            String accessRole
    ) {
    }
}
