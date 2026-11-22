package kr.co.daeun.flowmatebe.domain.task.repository;

import kr.co.daeun.flowmatebe.domain.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @Query("""
            select tc
            from TaskComment tc
            join fetch tc.author author
            join fetch author.user
            where tc.task.id = :taskId
            order by tc.createdAt, tc.id
            """)
    List<TaskComment> findAllWithAuthorByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("delete from TaskComment tc where tc.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);

    @Query("""
            select tc.task.id as taskId, count(tc.id) as commentCount
            from TaskComment tc
            where tc.task.id in :taskIds
            group by tc.task.id
            """)
    List<TaskCommentCount> countByTaskIds(@Param("taskIds") List<Long> taskIds);

    interface TaskCommentCount {

        Long getTaskId();

        long getCommentCount();
    }
}
