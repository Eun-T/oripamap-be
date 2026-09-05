package org.scoula.comment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.comment.vo.CommentVO;
import java.util.List;

@Mapper
public interface CommentMapper {

    List<CommentVO> findByPlaceId(Long placeId);

    int insertComment(
            @Param("placeId") Long placeId,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    int updateComment(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    int insertReply(
            @Param("parentCommentId") Long parentCommentId,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    int deleteComment(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );
}
