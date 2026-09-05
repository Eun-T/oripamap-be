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
    List<CommentVO> comments = List.of();
    List<CommentVO> photos = List.of();
    Object[] insertArgs;

    final CommentMapper mapper = (CommentMapper) Proxy.newProxyInstance(
            CommentMapper.class.getClassLoader(), new Class<?>[]{CommentMapper.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "insertComment":
                        events.add("insert");
                        insertArgs = args;
                        if (insertFailure != null) throw insertFailure;
                        return inserted;
                    case "insertReply": events.add("reply"); return 1;
                    case "updateComment": events.add("update"); return 1;
                    case "findOwnedByIdForUpdate": return owned;
                    case "deleteComment":
                        events.add("db-delete");
                        if (failDbDelete) throw new IllegalStateException("DB delete failed");
                        return 1;
                    case "findByPlaceId": return comments;
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
}
