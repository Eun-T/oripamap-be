package org.scoula.comment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.comment.vo.CommentVO;
import java.util.List;

@Mapper
public interface CommentMapper {

    List<CommentVO> findParentsByPlaceId(
            @Param("placeId") Long placeId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    List<CommentVO> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    long countByPlaceId(@Param("placeId") Long placeId);

    List<CommentVO> findPhotosByPlaceId(@Param("placeId") Long placeId);

    CommentVO findById(@Param("commentId") Long commentId);

    CommentVO findOwnedByIdForUpdate(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int insertComment(CommentVO comment);

    int updateComment(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    int insertReply(CommentVO comment);

    int deleteComment(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );
}
