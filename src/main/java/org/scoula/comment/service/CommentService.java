package org.scoula.comment.service;

import lombok.RequiredArgsConstructor;
import org.scoula.comment.dto.CommentResponse;
import org.scoula.comment.mapper.CommentMapper;
import org.scoula.comment.vo.CommentVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public List<CommentResponse> getComments(Long placeId) {

        return commentMapper.findByPlaceId(placeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void addComment(Long placeId, Long userId, String content) {

        commentMapper.insertComment(
                placeId,
                userId,
                content
        );
    }

    public boolean deleteComment(Long commentId, Long userId) {

        return commentMapper.deleteComment(
                commentId,
                userId
        ) > 0;
    }

    private CommentResponse toResponse(CommentVO vo) {

        return CommentResponse.builder()
                .id(vo.getId())
                .userId(vo.getUserId())
                .nickname(vo.getNickname())
                .content(vo.getContent())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }
}
