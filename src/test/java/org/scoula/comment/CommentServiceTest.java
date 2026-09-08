package org.scoula.comment;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommentServiceTest {
    private final CommentFixture f = new CommentFixture();
    private final MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1});

    @Test void textCommentStoresNullAndDoesNotCallS3() {
        f.service.addComment(1L, 7L, "text");
        assertNull(f.insertArgs[3]);
        assertEquals(List.of("begin", "insert", "created-query", "commit"), f.events);
    }

    @Test void imageCommentStoresOnlyKeyAfterUpload() {
        f.service.addComment(1L, 7L, "text", List.of(file));
        assertEquals(CommentFixture.KEY, f.insertArgs[3]);
        assertEquals(List.of("upload", "begin", "insert", "created-query", "commit", "sign"), f.events);
    }

    @Test void multipleImagesAndReplyImageAreRejectedBeforeSideEffects() {
        assertThrows(ResponseStatusException.class, () -> f.service.addComment(1L, 7L, "text", List.of(file, file)));
        assertThrows(ResponseStatusException.class, () -> f.service.addReply(1L, 7L, "text", List.of(file)));
        assertTrue(f.events.isEmpty());
    }

    @Test void dbFailureRollsBackThenDeletesUploadedObject() {
        f.insertFailure = new IllegalStateException("DB failed");
        assertSame(f.insertFailure, assertThrows(IllegalStateException.class,
                () -> f.service.addComment(1L, 7L, "text", List.of(file))));
        assertEquals(List.of("upload", "begin", "insert", "rollback", "s3-delete"), f.events);
    }

    @Test void zeroInsertedRowsAlsoCleansUp() {
        f.inserted = 0;
        assertThrows(IllegalStateException.class, () -> f.service.addComment(1L, 7L, "text", List.of(file)));
        assertTrue(f.events.contains("s3-delete"));
    }

    @Test void cleanupFailurePreservesOriginalDatabaseError() {
        f.insertFailure = new IllegalStateException("DB failed");
        f.failCleanup = true;
        var error = assertThrows(IllegalStateException.class, () -> f.service.addComment(1L, 7L, "text", List.of(file)));
        assertSame(f.insertFailure, error);
        assertEquals(1, error.getSuppressed().length);
    }

    @Test void unknownCommitOutcomeDoesNotDeletePossiblyReferencedImage() {
        f.failCommit = true;
        assertThrows(RuntimeException.class, () -> f.service.addComment(1L, 7L, "text", List.of(file)));
        assertFalse(f.events.contains("s3-delete"));
    }

    @Test void uploadFailureDoesNotWriteDatabase() {
        f.failUpload = true;
        assertThrows(IllegalStateException.class, () -> f.service.addComment(1L, 7L, "text", List.of(file)));
        assertEquals(List.of("upload"), f.events);
    }

    @Test void contentLimitUsesUnicodeCharactersAndCoversEveryWritePath() {
        String validEmojiContent = "😀".repeat(300);
        String tooLong = validEmojiContent + "a";

        f.service.addComment(1L, 7L, validEmojiContent);
        f.events.clear();

        assertThrows(ResponseStatusException.class, () -> f.service.addComment(1L, 7L, tooLong, List.of(file)));
        assertThrows(ResponseStatusException.class, () -> f.service.addReply(1L, 7L, tooLong));
        assertThrows(ResponseStatusException.class, () -> f.service.updateComment(1L, 7L, tooLong));
        assertTrue(f.events.isEmpty());
    }

    @Test void imageDeletionHappensAfterDatabaseCommit() {
        f.owned = CommentFixture.comment(1L, null, CommentFixture.KEY);
        assertTrue(f.service.deleteComment(1L, 7L));
        assertEquals(List.of("begin", "db-delete", "commit", "s3-delete"), f.events);
    }

    @Test void unauthorizedDeletionDoesNotTouchDatabaseOrS3() {
        assertFalse(f.service.deleteComment(1L, 8L));
        assertEquals(List.of("begin", "commit"), f.events);
    }

    @Test void databaseDeleteFailurePreservesObject() {
        f.owned = CommentFixture.comment(1L, null, CommentFixture.KEY);
        f.failDbDelete = true;
        assertThrows(RuntimeException.class, () -> f.service.deleteComment(1L, 7L));
        assertFalse(f.events.contains("s3-delete"));
    }

    @Test void deleteCommitFailurePreservesObject() {
        f.owned = CommentFixture.comment(1L, null, CommentFixture.KEY);
        f.failCommit = true;
        assertThrows(RuntimeException.class, () -> f.service.deleteComment(1L, 7L));
        assertFalse(f.events.contains("s3-delete"));
    }

    @Test void s3DeleteFailureIsReportedAfterCommentDeletion() {
        f.owned = CommentFixture.comment(1L, null, CommentFixture.KEY);
        f.failCleanup = true;
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> f.service.deleteComment(1L, 7L)).getRawStatusCode());
        assertTrue(f.events.indexOf("commit") < f.events.indexOf("s3-delete"));
    }

    @Test void textReplyUpdateAndDeleteKeepWorkingWithoutS3() {
        assertNotNull(f.service.addReply(1L, 7L, "reply"));
        assertTrue(f.service.updateComment(1L, 7L, "edited"));
        f.owned = CommentFixture.comment(2L, 1L, null);
        assertTrue(f.service.deleteComment(2L, 7L));
        assertFalse(f.events.contains("s3-delete"));
    }

    @Test void commentsKeepReplyTreeAndSignOnlyAttachedImages() {
        f.parents = List.of(CommentFixture.comment(1L, null, CommentFixture.KEY),
                CommentFixture.comment(3L, null, null));
        f.replies = List.of(CommentFixture.comment(2L, 1L, null));
        f.totalCount = 3;
        var result = f.service.getComments(1L, 0, 5);
        assertEquals(2, result.getComments().size());
        assertNotNull(result.getComments().get(0).getImageUrl());
        assertEquals(2L, result.getComments().get(0).getReplies().get(0).getId());
        assertNull(result.getComments().get(0).getReplies().get(0).getImageUrl());
        assertNull(result.getComments().get(1).getImageUrl());
        assertFalse(result.isHasNext());
        assertEquals(3, result.getTotalCount());
        assertEquals(List.of(1L, 3L), f.queriedParentIds);
        assertEquals(List.of("parents-query", "replies-query", "count-query", "sign"), f.events);
    }

    @Test void parentPaginationUsesExtraRowForHasNextAndFetchesOnlyVisibleReplies() {
        f.parents = List.of(
                CommentFixture.comment(10L, null, null),
                CommentFixture.comment(9L, null, null),
                CommentFixture.comment(8L, null, null),
                CommentFixture.comment(7L, null, null),
                CommentFixture.comment(6L, null, null),
                CommentFixture.comment(5L, null, null));

        var result = f.service.getComments(1L, 2, 5);

        assertEquals(5, result.getComments().size());
        assertTrue(result.isHasNext());
        assertEquals(2, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(10L, f.queryOffset);
        assertEquals(6, f.queryLimit);
        assertEquals(List.of(10L, 9L, 8L, 7L, 6L), f.queriedParentIds);
    }

    @Test void emptyParentPageDoesNotRunReplyQuery() {
        f.totalCount = 27;
        var result = f.service.getComments(1L, 0, 5);
        assertTrue(result.getComments().isEmpty());
        assertFalse(result.isHasNext());
        assertEquals(27, result.getTotalCount());
        assertEquals(List.of("parents-query", "count-query"), f.events);
    }

    @Test void invalidPaginationIsRejectedBeforeQuery() {
        assertThrows(ResponseStatusException.class, () -> f.service.getComments(1L, -1, 5));
        assertThrows(ResponseStatusException.class, () -> f.service.getComments(1L, 0, 0));
        assertThrows(ResponseStatusException.class, () -> f.service.getComments(1L, 0, 101));
        assertTrue(f.events.isEmpty());
    }

    @Test void visitorPhotosIncludeFreshUrls() {
        f.photos = List.of(CommentFixture.comment(1L, null, CommentFixture.KEY));
        assertNotNull(f.service.getPhotos(1L).get(0).getImageUrl());
    }
}
