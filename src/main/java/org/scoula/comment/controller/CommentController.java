package org.scoula.comment.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.comment.dto.CommentRequest;
import org.scoula.comment.dto.CommentResponse;
import org.scoula.comment.service.CommentService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/place/{placeId}/photos")
    public List<CommentResponse> getPhotos(@PathVariable Long placeId) {
        return commentService.getPhotos(placeId);
    }

    // 기존 JSON 댓글 작성
    @PostMapping(value = "/place/{placeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(value = "/place/{placeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addCommentWithImage(
            @PathVariable Long placeId,
            @RequestParam(value = "content", required = false) String content,
            MultipartHttpServletRequest request,
            @AuthenticationPrincipal CustomUser user) {
        commentService.addComment(placeId, user.getMember().getId(), content, files(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 답글 작성
    @PostMapping(value = "/{commentId}/replies", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addReply(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUser user
    ) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean created = commentService.addReply(
                commentId,
                user.getMember().getId(),
                request.getContent().trim()
        );
        return created
                ? ResponseEntity.status(HttpStatus.CREATED).build()
                : ResponseEntity.badRequest().build();
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUser user
    ) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean updated = commentService.updateComment(
                commentId,
                user.getMember().getId(),
                request.getContent().trim()
        );
        return updated
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping(value = "/{commentId}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addReplyMultipart(
            @PathVariable Long commentId,
            @RequestParam(value = "content", required = false) String content,
            MultipartHttpServletRequest request,
            @AuthenticationPrincipal CustomUser user) {
        boolean created = commentService.addReply(commentId, user.getMember().getId(),
                content == null ? null : content.trim(), files(request));
        return created ? ResponseEntity.status(HttpStatus.CREATED).build() : ResponseEntity.badRequest().build();
    }

    private List<MultipartFile> files(MultipartHttpServletRequest request) {
        if (request.getMultiFileMap().keySet().stream().anyMatch(name -> !name.equals("file"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 file 필드로 전송해 주세요.");
        }
        return request.getFiles("file");
    }

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
