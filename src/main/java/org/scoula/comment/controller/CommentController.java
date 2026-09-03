package org.scoula.comment.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.comment.dto.CommentRequest;
import org.scoula.comment.dto.CommentResponse;
import org.scoula.comment.service.CommentService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<Void> addComment(
            @PathVariable Long placeId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUser user
    ) {
        commentService.addComment(
                placeId,
                user.getMember().getId(),
                request.getContent()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        boolean deleted = commentService.deleteComment(
                commentId,
                user.getMember().getId()
        );
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
