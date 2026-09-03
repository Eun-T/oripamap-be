package org.scoula.comment.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.comment.dto.CommentRequest;
import org.scoula.comment.dto.CommentResponse;
import org.scoula.comment.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 목록
    @GetMapping("/place/{placeId}")
    public List<CommentResponse> getComments(
            @PathVariable Long placeId
    ) {
        return commentService.getComments(placeId);
    }

    // 댓글 작성
    @PostMapping("/place/{placeId}")
    public void addComment(
            @PathVariable Long placeId,
            @RequestBody CommentRequest request
    ) {
        commentService.addComment(
                placeId,
                request.getContent()
        );
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);
    }
}