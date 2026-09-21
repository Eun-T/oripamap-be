package org.scoula.member.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.scoula.common.service.S3ImageService;
import org.scoula.member.mapper.MemberWithdrawalMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberWithdrawalServiceTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesOwnedDataInFkSafeOrderAndCleansImagesOnlyAfterCommit() {
        RecordingMapper mapper = new RecordingMapper();
        RecordingS3ImageService s3 = new RecordingS3ImageService();
        MemberWithdrawalService service = new MemberWithdrawalService(mapper, s3);
        TransactionSynchronizationManager.initSynchronization();

        service.withdraw(7L);

        assertEquals(List.of(
                "lock-user", "find-images", "detach-replies", "comments", "favorites",
                "inquiries", "edit-requests", "refresh-tokens", "user"
        ), mapper.calls);
        assertEquals(List.of(), s3.deletedKeys);

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertEquals(List.of("images/11111111-1111-1111-1111-111111111111.jpg"),
                s3.deletedKeys);
    }

    @Test
    void rejectsAlreadyDeletedUserBeforeDeletingRelatedData() {
        RecordingMapper mapper = new RecordingMapper();
        mapper.existingUserId = null;
        MemberWithdrawalService service = new MemberWithdrawalService(
                mapper, new RecordingS3ImageService());
        TransactionSynchronizationManager.initSynchronization();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.withdraw(7L));

        assertEquals(404, exception.getRawStatusCode());
        assertEquals(List.of("lock-user"), mapper.calls);
    }

    private static class RecordingS3ImageService extends S3ImageService {
        private final List<String> deletedKeys = new ArrayList<>();

        private RecordingS3ImageService() {
            super(null, null, "test-bucket");
        }

        @Override
        public void delete(String key) {
            deletedKeys.add(key);
        }
    }

    private static class RecordingMapper implements MemberWithdrawalMapper {
        private final List<String> calls = new ArrayList<>();
        private Long existingUserId = 7L;

        @Override
        public Long findUserIdForUpdate(Long userId) {
            calls.add("lock-user");
            return existingUserId;
        }

        @Override
        public List<String> findCommentImageKeysByUserId(Long userId) {
            calls.add("find-images");
            return List.of("images/11111111-1111-1111-1111-111111111111.jpg");
        }

        @Override
        public int detachOtherUsersReplies(Long userId) {
            calls.add("detach-replies");
            return 1;
        }

        @Override
        public int deleteCommentsByUserId(Long userId) {
            calls.add("comments");
            return 1;
        }

        @Override
        public int deleteFavoritesByUserId(Long userId) {
            calls.add("favorites");
            return 1;
        }

        @Override
        public int deleteInquiriesByUserId(Long userId) {
            calls.add("inquiries");
            return 1;
        }

        @Override
        public int deleteEditRequestsByUserId(Long userId) {
            calls.add("edit-requests");
            return 1;
        }

        @Override
        public int deleteRefreshTokensByUserId(Long userId) {
            calls.add("refresh-tokens");
            return 1;
        }

        @Override
        public int deleteUser(Long userId) {
            calls.add("user");
            return 1;
        }
    }
}
