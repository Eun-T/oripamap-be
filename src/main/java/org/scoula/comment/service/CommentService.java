package org.scoula.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.service.S3ImageService;
import org.scoula.comment.dto.CommentResponse;
import org.scoula.comment.mapper.CommentMapper;
import org.scoula.comment.vo.CommentVO;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CommentService {

    private final CommentMapper commentMapper;
    private final S3ImageService s3ImageService;
    private final PlatformTransactionManager transactionManager;

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
        addComment(placeId, userId, content, Collections.emptyList());
    }

    public void addComment(Long placeId, Long userId, String content, List<MultipartFile> files) {
        if (files.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글 이미지는 최대 1장만 첨부할 수 있습니다.");
        }
        // 기존 본문 동작은 유지하되 DB NOT NULL 위반 전에 확인한다.
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글 내용을 입력해 주세요.");
        }
        String imageKey = files.isEmpty() ? null : s3ImageService.upload(files.get(0));
        // UNKNOWN(커밋 결과 불명)일 때는 저장된 댓글의 이미지를 잘못 삭제하지 않는다.
        int[] completion = {TransactionSynchronization.STATUS_ROLLED_BACK};
        try {
            transaction().executeWithoutResult(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int result) {
                        completion[0] = result;
                    }
                });
                if (commentMapper.insertComment(placeId, userId, content, imageKey) != 1) {
                    throw new IllegalStateException("댓글 저장에 실패했습니다.");
                }
            });
        } catch (RuntimeException e) {
            if (imageKey != null && completion[0] == TransactionSynchronization.STATUS_ROLLED_BACK) {
                try {
                    s3ImageService.delete(imageKey);
                } catch (RuntimeException cleanupError) {
                    e.addSuppressed(cleanupError);
                    log.error("댓글 저장 실패 후 S3 이미지 정리 실패: key={}", imageKey, cleanupError);
                }
            } else if (imageKey != null) {
                log.error("댓글 커밋 결과 확인 필요. S3 이미지 유지: key={}", imageKey, e);
            }
            throw e;
        }
    }

    public List<CommentResponse> getPhotos(Long placeId) {
        return commentMapper.findPhotosByPlaceId(placeId).stream().map(this::toResponse).toList();
    }

    public boolean updateComment(Long commentId, Long userId, String content) {
        return commentMapper.updateComment(commentId, userId, content) > 0;
    }

    public boolean addReply(Long parentCommentId, Long userId, String content) {
        return addReply(parentCommentId, userId, content, Collections.emptyList());
    }

    public boolean addReply(Long parentCommentId, Long userId, String content, List<MultipartFile> files) {
        if (!files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답글에는 이미지를 첨부할 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            return false;
        }
        return commentMapper.insertReply(parentCommentId, userId, content) > 0;
    }

    public boolean deleteComment(Long commentId, Long userId) {
        CommentVO deleted = transaction().execute(status -> {
            CommentVO comment = commentMapper.findOwnedByIdForUpdate(commentId, userId);
            if (comment == null || commentMapper.deleteComment(commentId, userId) == 0) {
                return null;
            }
            return comment;
        });
        if (deleted == null) {
            return false;
        }
        // DB 커밋이 끝난 뒤 삭제한다. DB 롤백 시 기존 이미지가 유실되지 않는다.
        if (deleted.getImageKey() != null) {
            try {
                s3ImageService.delete(deleted.getImageKey());
            } catch (RuntimeException e) {
                log.error("댓글은 삭제됐으나 S3 이미지 정리 실패: commentId={}, key={}",
                        commentId, deleted.getImageKey(), e);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "댓글은 삭제되었지만 이미지 파일 정리에 실패했습니다.", e);
            }
        }
        return true;
    }

    private TransactionTemplate transaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        // 상위 트랜잭션 유무와 무관하게 DB 커밋/롤백을 확인한 뒤 S3를 정리한다.
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
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
                .imageUrl(vo.getImageKey() == null ? null : s3ImageService.createPresignedGetUrl(vo.getImageKey()))
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .replies(replies)
                .build();
    }
}
