package org.scoula.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scoula.comment.controller.CommentController;
import org.scoula.exception.ApiExceptionAdvice;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class CommentControllerTest {
    final CommentFixture f = new CommentFixture();
    MockMvc mvc;

    @BeforeEach void setUp() {
        CustomUser user = new CustomUser(MemberVO.builder().id(7L).username("tester")
                .password("unused").authList(List.of()).build());
        mvc = MockMvcBuilders.standaloneSetup(new CommentController(f.service))
                .setControllerAdvice(new ApiExceptionAdvice())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CustomUser.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory factory) { return user; }
                }).build();
    }

    @Test void originalJsonCommentAndReplyStillReturn201() throws Exception {
        for (String path : List.of("/api/comments/place/1", "/api/comments/1/replies")) {
            assertEquals(201, mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"hello\"}")).andReturn().getResponse().getStatus());
        }
        assertNull(f.insertArgs[3]);
        assertFalse(f.events.contains("upload"));
    }

    @Test void multipartImageCommentReturns201AndStoresKey() throws Exception {
        assertEquals(201, mvc.perform(multipart("/api/comments/place/1")
                .file(file("file")).param("content", "photo")).andReturn().getResponse().getStatus());
        assertEquals(CommentFixture.KEY, f.insertArgs[3]);
    }

    @Test void multipartWithoutImageStillWorks() throws Exception {
        assertEquals(201, mvc.perform(multipart("/api/comments/place/1")
                .param("content", "text")).andReturn().getResponse().getStatus());
        assertNull(f.insertArgs[3]);
    }

    @Test void repeatedFilesAndUnexpectedFileFieldsReturn400() throws Exception {
        assertEquals(400, mvc.perform(multipart("/api/comments/place/1")
                .file(file("file")).file(file("file")).param("content", "text")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(multipart("/api/comments/place/1")
                .file(file("file")).file(file("extra")).param("content", "text")).andReturn().getResponse().getStatus());
        assertTrue(f.events.isEmpty());
    }

    @Test void replyFileIsRejectedBeforeUploadOrInsert() throws Exception {
        assertEquals(400, mvc.perform(multipart("/api/comments/1/replies")
                .file(file("file")).param("content", "text")).andReturn().getResponse().getStatus());
        assertTrue(f.events.isEmpty());
    }

    @Test void jsonCannotAttachExistingObjectKey() throws Exception {
        assertEquals(400, mvc.perform(post("/api/comments/1/replies").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"text\",\"imageKey\":\"images/other.png\"}"))
                .andReturn().getResponse().getStatus());
        assertTrue(f.events.isEmpty());
    }

    @Test void visitorPhotoEndpointReturnsUrlsWithoutExposingStorageKeys() throws Exception {
        f.photos = List.of(CommentFixture.comment(1L, null, CommentFixture.KEY));
        var response = mvc.perform(get("/api/comments/place/1/photos")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"imageUrl\":\"https://"));
        assertFalse(response.getContentAsString().contains("\"imageKey\""));
    }

    @Test void updateAndDeleteKeepExistingStatuses() throws Exception {
        assertEquals(204, mvc.perform(put("/api/comments/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"edited\"}")).andReturn().getResponse().getStatus());
        f.owned = CommentFixture.comment(1L, null, CommentFixture.KEY);
        assertEquals(204, mvc.perform(delete("/api/comments/1")).andReturn().getResponse().getStatus());
        assertTrue(f.events.contains("s3-delete"));
    }

    private MockMultipartFile file(String field) {
        return new MockMultipartFile(field, "test.png", "image/png", new byte[]{1});
    }
}
