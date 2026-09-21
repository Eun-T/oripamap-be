package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.service.S3ImageService;
import org.scoula.member.mapper.MemberWithdrawalMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class MemberWithdrawalService {
    private final MemberWithdrawalMapper mapper;
    private final S3ImageService s3ImageService;

    @Transactional
    public void withdraw(Long userId) {
        if (userId == null || mapper.findUserIdForUpdate(userId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        List<String> commentImageKeys = mapper.findCommentImageKeysByUserId(userId);
        registerCommentImageCleanupAfterCommit(userId, commentImageKeys);

        mapper.detachOtherUsersReplies(userId);
        mapper.deleteCommentsByUserId(userId);
        mapper.deleteFavoritesByUserId(userId);
        mapper.deleteInquiriesByUserId(userId);
        mapper.deleteEditRequestsByUserId(userId);
        mapper.deleteRefreshTokensByUserId(userId);

        if (mapper.deleteUser(userId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
    }

    private void registerCommentImageCleanupAfterCommit(Long userId, List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("회원 탈퇴는 활성 트랜잭션 안에서 실행되어야 합니다.");
        }
        List<String> keys = List.copyOf(imageKeys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : keys) {
                    try {
                        s3ImageService.delete(key);
                    } catch (RuntimeException e) {
                        // DB는 이미 일관되게 커밋됐다. 나머지 객체 삭제를 계속하고 운영 로그로 재처리한다.
                        log.error("회원 탈퇴 후 댓글 S3 이미지 삭제 실패: userId={}, key={}", userId, key, e);
                    }
                }
            }
        });
    }
}
