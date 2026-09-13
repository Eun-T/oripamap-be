package org.scoula.inquiry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scoula.exception.ApiExceptionAdvice;
import org.scoula.inquiry.controller.AdminInquiryController;
import org.scoula.inquiry.controller.InquiryController;
import org.scoula.inquiry.mapper.InquiryMapper;
import org.scoula.inquiry.service.InquiryService;
import org.scoula.inquiry.vo.InquiryStatus;
import org.scoula.inquiry.vo.InquiryType;
import org.scoula.inquiry.vo.InquiryVO;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class InquiryControllerTest {

    private TrackingInquiryMapper mapper;
    private MockMvc mvc;
    private MockMvc adminMvc;

    @BeforeEach
    void setUp() {
        mapper = new TrackingInquiryMapper();
        CustomUser user = new CustomUser(MemberVO.builder()
                .id(42L)
                .username("member@example.com")
                .password("unused")
                .authList(List.of())
                .build());

        mvc = MockMvcBuilders.standaloneSetup(
                        new InquiryController(new InquiryService(mapper)))
                .setControllerAdvice(new ApiExceptionAdvice())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CustomUser.class;
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer container,
                                                  NativeWebRequest request,
                                                  WebDataBinderFactory factory) {
                        return user;
                    }
                })
                .build();
        adminMvc = MockMvcBuilders.standaloneSetup(
                        new AdminInquiryController(new InquiryService(mapper)))
                .setControllerAdvice(new ApiExceptionAdvice())
                .build();
    }

    @Test
    void createsPendingInquiryForAuthenticatedUser() throws Exception {
        var response = mvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"STORE_REGISTRATION\",\"title\":\" 홍대 매장 등록 요청 \",\"content\":\" 매장명은 ... 입니다. \"}"))
                .andReturn().getResponse();

        assertEquals(201, response.getStatus());
        assertEquals(42L, mapper.saved.getUserId());
        assertEquals(InquiryStatus.PENDING, mapper.saved.getStatus());
        assertEquals("홍대 매장 등록 요청", mapper.saved.getTitle());
        assertEquals("매장명은 ... 입니다.", mapper.saved.getContent());

        String body = response.getContentAsString();
        assertTrue(body.contains("\"id\":1"));
        assertTrue(body.contains("\"type\":\"STORE_REGISTRATION\""));
        assertTrue(body.contains("\"status\":\"PENDING\""));
        assertTrue(body.contains("\"createdAt\":\"2026-09-12T12:00:00\""));
        assertFalse(body.contains("userId"));
        assertFalse(body.contains("title"));
        assertFalse(body.contains("content"));
    }

    @Test
    void invalidTypeReturns400() throws Exception {
        int status = mvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"UNKNOWN\",\"title\":\"제목\",\"content\":\"내용\"}"))
                .andReturn().getResponse().getStatus();

        assertEquals(400, status);
    }

    @Test
    void requestCannotOverrideUserIdOrPendingStatus() throws Exception {
        int status = mvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUG\",\"title\":\"제목\",\"content\":\"내용\",\"userId\":999,\"status\":\"RESOLVED\"}"))
                .andReturn().getResponse().getStatus();

        assertEquals(201, status);
        assertEquals(42L, mapper.saved.getUserId());
        assertEquals(InquiryStatus.PENDING, mapper.saved.getStatus());
    }

    @Test
    void validatesRequiredAndMaximumLengths() throws Exception {
        assertEquals(400, request("GENERAL", "   ", "내용"));
        assertEquals(400, request("GENERAL", "제목", "   "));
        assertEquals(201, request("GENERAL", "가".repeat(100), "나".repeat(2000)));
        assertEquals(400, request("GENERAL", "가".repeat(101), "내용"));
        assertEquals(400, request("GENERAL", "제목", "나".repeat(2001)));
    }

    @Test
    void listsAuthenticatedUsersInquiriesWithIsoCreatedAt() throws Exception {
        mapper.inquiries = List.of(
                inquiry(3L, "매장 정보 수정 요청", LocalDateTime.of(2026, 9, 12, 21, 52)),
                inquiry(1L, "이전 문의", LocalDateTime.of(2026, 9, 10, 9, 30))
        );

        var response = mvc.perform(get("/api/inquiries/me"))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertEquals(42L, mapper.requestedUserId);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"id\":3"));
        assertTrue(body.contains("\"type\":\"INFO_CORRECTION\""));
        assertTrue(body.contains("\"title\":\"매장 정보 수정 요청\""));
        assertTrue(body.contains("\"status\":\"PENDING\""));
        assertTrue(body.contains("\"createdAt\":\"2026-09-12T21:52:00\""));
    }

    @Test
    void getsOnlyAuthenticatedUsersInquiryDetail() throws Exception {
        InquiryVO detail = inquiry(3L, "영업시간 수정 요청", LocalDateTime.of(2026, 9, 12, 21, 52));
        detail.setContent("영업시간이 잘못되어 있습니다.");
        detail.setStatus(InquiryStatus.RESOLVED);
        detail.setAnswer("확인 후 수정했습니다.");
        detail.setAnsweredAt(LocalDateTime.of(2026, 9, 12, 22, 30));
        mapper.detail = detail;

        var response = mvc.perform(get("/api/inquiries/3"))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertEquals(3L, mapper.requestedInquiryId);
        assertEquals(42L, mapper.requestedUserId);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"content\":\"영업시간이 잘못되어 있습니다.\""));
        assertTrue(body.contains("\"answer\":\"확인 후 수정했습니다.\""));
        assertTrue(body.contains("\"createdAt\":\"2026-09-12T21:52:00\""));
        assertTrue(body.contains("\"answeredAt\":\"2026-09-12T22:30:00\""));
    }

    @Test
    void missingOrOtherUsersInquiryDetailReturns404() throws Exception {
        assertEquals(404, mvc.perform(get("/api/inquiries/3"))
                .andReturn().getResponse().getStatus());
        assertEquals(42L, mapper.requestedUserId);
    }

    @Test
    void adminPutCreatesAndOverwritesAnswer() throws Exception {
        mapper.existing = inquiry(3L, "제목", LocalDateTime.now());

        for (String answer : List.of(" 최초 답변 ", "수정 답변")) {
            int status = adminMvc.perform(put("/api/admin/inquiries/3/answer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answer\":\"" + answer + "\"}"))
                    .andReturn().getResponse().getStatus();
            assertEquals(204, status);
        }

        assertEquals(2, mapper.answerUpdateCount);
        assertEquals("수정 답변", mapper.updatedAnswer);
    }

    @Test
    void adminAnswerValidatesBodyAndInquiryExistence() throws Exception {
        assertEquals(400, answer("   "));
        assertEquals(400, answer("가".repeat(2001)));
        assertEquals(404, answer("답변"));
        assertEquals(0, mapper.answerUpdateCount);
    }

    private int answer(String answer) throws Exception {
        return adminMvc.perform(put("/api/admin/inquiries/3/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"" + answer + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private InquiryVO inquiry(Long id, String title, LocalDateTime createdAt) {
        InquiryVO inquiry = new InquiryVO();
        inquiry.setId(id);
        inquiry.setType(InquiryType.INFO_CORRECTION);
        inquiry.setTitle(title);
        inquiry.setStatus(InquiryStatus.PENDING);
        inquiry.setCreatedAt(createdAt);
        return inquiry;
    }

    private int request(String type, String title, String content) throws Exception {
        String json = String.format("{\"type\":\"%s\",\"title\":\"%s\",\"content\":\"%s\"}",
                type, title, content);
        return mvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getStatus();
    }

    private static class TrackingInquiryMapper implements InquiryMapper {
        private InquiryVO saved;
        private Long requestedUserId;
        private Long requestedInquiryId;
        private List<InquiryVO> inquiries = List.of();
        private InquiryVO detail;
        private InquiryVO existing;
        private String updatedAnswer;
        private int answerUpdateCount;

        @Override
        public int insert(InquiryVO inquiry) {
            inquiry.setId(1L);
            saved = inquiry;
            return 1;
        }

        @Override
        public InquiryVO findById(Long id) {
            if (existing != null) {
                return existing;
            }
            if (saved != null) {
                saved.setCreatedAt(LocalDateTime.of(2026, 9, 12, 12, 0));
            }
            return saved;
        }

        @Override
        public List<InquiryVO> findAllByUserId(Long userId) {
            requestedUserId = userId;
            return inquiries;
        }

        @Override
        public InquiryVO findByIdAndUserId(Long id, Long userId) {
            requestedInquiryId = id;
            requestedUserId = userId;
            return detail;
        }

        @Override
        public int updateAnswer(Long id, String answer) {
            updatedAnswer = answer;
            answerUpdateCount++;
            return existing == null ? 0 : 1;
        }
    }
}
