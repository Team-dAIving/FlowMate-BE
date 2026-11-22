package kr.co.daeun.flowmatebe.domain.task.dto.response;

import kr.co.daeun.flowmatebe.domain.task.entity.TaskComment;
import kr.co.daeun.flowmatebe.domain.user.entity.User;

import java.time.OffsetDateTime;

public record TaskCommentResponse(
        Long id,
        Long authorUserId,
        String authorNickname,
        String authorAvatarUrl,
        String content,
        OffsetDateTime createdAt,
        boolean mine
) {

    public static TaskCommentResponse of(TaskComment comment, Long currentUserId) {
        User author = comment.getAuthor().getUser();
        return new TaskCommentResponse(
                comment.getId(),
                author.getId(),
                author.getNickname(),
                author.getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                author.getId().equals(currentUserId)
        );
    }
}
