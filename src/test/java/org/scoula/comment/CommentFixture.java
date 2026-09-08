package org.scoula.comment;

import org.scoula.comment.mapper.CommentMapper;
import org.scoula.comment.service.CommentService;
import org.scoula.comment.vo.CommentVO;
import org.scoula.common.service.S3ImageService;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class CommentFixture {
    static final String KEY = "images/12345678-1234-1234-1234-123456789abc.png";
    final List<String> events = new ArrayList<>();
    RuntimeException insertFailure;
    boolean failCommit;
    boolean failCleanup;
    boolean failUpload;
    boolean failDbDelete;
    int inserted = 1;
    CommentVO owned;
    List<CommentVO> parents = List.of();
    List<CommentVO> replies = List.of();
    List<CommentVO> photos = List.of();
    List<Long> queriedParentIds = List.of();
    long queryOffset;
    int queryLimit;
    long totalCount;
    Object[] insertArgs;
    CommentVO created;

    final CommentMapper mapper = (CommentMapper) Proxy.newProxyInstance(
            CommentMapper.class.getClassLoader(), new Class<?>[]{CommentMapper.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "insertComment":
                        events.add("insert");
                        CommentVO comment = (CommentVO) args[0];
                        insertArgs = new Object[]{comment.getPlaceId(), comment.getUserId(),
                                comment.getContent(), comment.getImageKey()};
                        if (insertFailure != null) throw insertFailure;
                        if (inserted == 1) {
                            comment.setId(100L);
                            created = savedComment(comment);
                        }
                        return inserted;
                    case "insertReply":
                        events.add("reply");
                        CommentVO reply = (CommentVO) args[0];
                        reply.setId(101L);
                        created = savedComment(reply);
                        return 1;
                    case "findById":
                        events.add("created-query");
                        return created;
                    case "updateComment": events.add("update"); return 1;
                    case "findOwnedByIdForUpdate": return owned;
                    case "deleteComment":
                        events.add("db-delete");
                        if (failDbDelete) throw new IllegalStateException("DB delete failed");
                        return 1;
                    case "findParentsByPlaceId":
                        events.add("parents-query");
                        queryOffset = (long) args[1];
                        queryLimit = (int) args[2];
                        return parents;
                    case "findRepliesByParentIds":
                        events.add("replies-query");
                        queriedParentIds = (List<Long>) args[0];
                        return replies;
                    case "countByPlaceId":
                        events.add("count-query");
                        return totalCount;
                    case "findPhotosByPlaceId": return photos;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });

    final S3ImageService s3 = new S3ImageService(null, null, "oripa-images") {
        @Override public String upload(MultipartFile file) {
            events.add("upload");
            if (failUpload) throw new IllegalStateException("S3 unavailable");
            return KEY;
        }
        @Override public void delete(String key) {
            if (!KEY.equals(key)) throw new AssertionError("Unexpected key " + key);
            events.add("s3-delete");
            if (failCleanup) throw new IllegalStateException("S3 deletion failed");
        }
        @Override public String createPresignedGetUrl(String key) {
            events.add("sign");
            return "https://example.test/" + key;
        }
    };

    final AbstractPlatformTransactionManager transactions = new AbstractPlatformTransactionManager() {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) { events.add("begin"); }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            events.add("commit");
            if (failCommit) throw new TransactionSystemException("Commit outcome unknown");
        }
        @Override protected void doRollback(DefaultTransactionStatus status) { events.add("rollback"); }
    };
    final CommentService service = new CommentService(mapper, s3, transactions);

    static CommentVO comment(long id, Long parent, String key) {
        CommentVO vo = new CommentVO();
        vo.setId(id);
        vo.setUserId(7L);
        vo.setParentCommentId(parent);
        vo.setImageKey(key);
        vo.setContent("기존 댓글");
        return vo;
    }

    private static CommentVO savedComment(CommentVO source) {
        CommentVO saved = new CommentVO();
        saved.setId(source.getId());
        saved.setPlaceId(source.getPlaceId());
        saved.setUserId(source.getUserId());
        saved.setParentCommentId(source.getParentCommentId());
        saved.setContent(source.getContent());
        saved.setImageKey(source.getImageKey());
        saved.setNickname("database-user");
        saved.setCreatedAt(LocalDateTime.of(2026, 9, 8, 12, 34, 56));
        saved.setUpdatedAt(saved.getCreatedAt());
        return saved;
    }
}
