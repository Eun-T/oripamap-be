package org.scoula.common.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scoula.common.service.S3ImageService;
import org.scoula.exception.ApiExceptionAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ImageControllerTest {
    private static final String KEY = "images/12345678-1234-1234-1234-123456789abc.png";
    private static final String URL = "https://example.test/image?signature=test";
    private MultipartFile receivedFile;
    private boolean failUpload;
    private boolean signed;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        S3ImageService service = new S3ImageService(null, null, "oripa-images") {
            @Override
            public String upload(MultipartFile file) {
                receivedFile = file;
                if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
                    return super.upload(file);
                }
                if (failUpload) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "이미지 업로드에 실패했습니다.");
                }
                return KEY;
            }

            @Override
            public String createPresignedGetUrl(String key) {
                assertEquals(KEY, key);
                signed = true;
                return URL;
            }
        };
        // MVC 요청 바인딩과 응답 검증용. 실제 S3 통신 및 인증 필터는 사용하지 않는다.
        mvc = MockMvcBuilders.standaloneSetup(new ImageController(service))
                .setControllerAdvice(new ApiExceptionAdvice()).build();
    }

    @Test
    void returnsCreatedWithKeyAndUrl() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3};
        var response = mvc.perform(multipart("/api/images")
                        .file(new MockMultipartFile("file", "test.png", "image/png", bytes)))
                .andReturn().getResponse();
        assertEquals(201, response.getStatus());
        JsonNode json = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals(KEY, json.get("key").asText());
        assertEquals(URL, json.get("url").asText());
        assertArrayEquals(bytes, receivedFile.getBytes());
        assertTrue(signed);
    }

    @Test
    void missingFileReturnsBadRequest() throws Exception {
        assertEquals(400, mvc.perform(multipart("/api/images")).andReturn().getResponse().getStatus());
        assertFalse(signed);
    }

    @Test
    void emptyFileReturnsBadRequest() throws Exception {
        assertEquals(400, mvc.perform(multipart("/api/images")
                .file(new MockMultipartFile("file", new byte[0])))
                .andReturn().getResponse().getStatus());
        assertFalse(signed);
    }

    @Test
    void oversizedFileReturnsPayloadTooLarge() throws Exception {
        assertEquals(413, mvc.perform(multipart("/api/images")
                .file(new MockMultipartFile("file", new byte[5 * 1024 * 1024 + 1])))
                .andReturn().getResponse().getStatus());
        assertFalse(signed);
    }

    @Test
    void jsonRequestReturnsUnsupportedMediaType() throws Exception {
        assertEquals(415, mvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus());
        assertFalse(signed);
    }

    @Test
    void uploadFailureDoesNotGenerateUrl() throws Exception {
        failUpload = true;
        assertEquals(502, mvc.perform(multipart("/api/images")
                .file(new MockMultipartFile("file", new byte[]{1})))
                .andReturn().getResponse().getStatus());
        assertFalse(signed);
    }
}
