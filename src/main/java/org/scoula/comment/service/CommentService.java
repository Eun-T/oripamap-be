package org.scoula.comment.service;

import lombok.RequiredArgsConstructor;
import org.scoula.comment.dto.CommentResponse;
import org.scoula.comment.mapper.CommentMapper;
import org.scoula.comment.vo.CommentVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public List<CommentResponse> getComments(Long placeId) {
        List<CommentVO> comments = commentMapper.findByPlaceId(placeId);
        Map<Long, List<CommentResponse>> repliesByParent = comments.stream()
                .filter(comment -> comment.getParentCommentId() != null)
                .collect(Collectors.groupingBy(
                        CommentVO::getParentCommentId,
                        Collectors.mapping(this::toResponse, Collectors.toList())
                ));

        return comments.stream()
                .filter(comment -> comment.getParentCommentId() == null)
                .map(comment -> toResponse(comment,
                        repliesByParent.getOrDefault(comment.getId(), Collections.emptyList())))
                .toList();
    }

    public void addComment(Long placeId, Long userId, String content) {

        commentMapper.insertComment(
                placeId,
                userId,
                content
        );
    }

    public boolean updateComment(Long commentId, Long userId, String content) {
        return commentMapper.updateComment(commentId, userId, content) > 0;
    }

    public boolean addReply(Long parentCommentId, Long userId, String content) {
        return commentMapper.insertReply(parentCommentId, userId, content) > 0;
    }

    public boolean deleteComment(Long commentId, Long userId) {

        return commentMapper.deleteComment(
                commentId,
                userId
        ) > 0;
    }

    private CommentResponse toResponse(CommentVO vo) {

        return toResponse(vo, Collections.emptyList());
    }

    private CommentResponse toResponse(CommentVO vo, List<CommentResponse> replies) {

        return CommentResponse.builder()
                .id(vo.getId())
                .userId(vo.getUserId())
                .parentCommentId(vo.getParentCommentId())
                .nickname(vo.getNickname())
                .content(vo.getContent())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .replies(replies)
                .build();
    }
}
