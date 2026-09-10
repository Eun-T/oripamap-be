package org.scoula.security.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.scoula.member.controller.UserController;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.account.dto.UserInfoDTO;
import org.scoula.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.security.handler.LoginSuccessHandler;
import org.scoula.security.refresh.mapper.RefreshTokenMapper;
import org.scoula.security.refresh.service.RefreshTokenService;
import org.scoula.security.service.LoginRateLimiter;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.JwtProcessor;
import org.scoula.security.util.RefreshTokenCookieUtil;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.reflect.Proxy;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserInfoResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void usersMeReturnsOwnersPlaceIdAndKeepsRolesFormat() throws Exception {
        CustomUser owner = user("OWNER", 3L, "KAKAO");

        UserInfoDTO body = new UserController(null).getMe(owner).getBody();
        JsonNode json = objectMapper.valueToTree(body);

        assertEquals(3L, json.get("placeId").asLong());
        assertEquals("ROLE_OWNER", json.get("roles").get(0).asText());
        assertEquals("KAKAO", json.get("provider").asText());
    }

    @Test
    void loginSuccessUsesTheSameResponseIncludingPlaceId() throws Exception {
        JwtProcessor jwtProcessor = new JwtProcessor(Base64.getEncoder().encodeToString(new byte[32]));
        RefreshTokenMapper refreshMapper = (RefreshTokenMapper) Proxy.newProxyInstance(
                RefreshTokenMapper.class.getClassLoader(), new Class<?>[]{RefreshTokenMapper.class},
                (proxy, method, args) -> "insert".equals(method.getName()) ? 1 : null);
        LoginSuccessHandler handler = new LoginSuccessHandler(
                jwtProcessor,
                new JwtCookieUtil(false),
                new RefreshTokenService(refreshMapper),
                new RefreshTokenCookieUtil(false),
                new LoginRateLimiter());
        CustomUser owner = user("OWNER", 3L, "LOCAL");
        var authentication = new UsernamePasswordAuthenticationToken(
                owner, null, owner.getAuthorities());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtUsernamePasswordAuthenticationFilter.LOGIN_EMAIL_ATTRIBUTE,
                owner.getUsername());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals(3L, json.get("placeId").asLong());
        assertEquals("ROLE_OWNER", json.get("roles").get(0).asText());
        assertEquals(2, response.getHeaders("Set-Cookie").size());
    }

    @Test
    void userAndAdminWithoutAssignmentReturnNullPlaceId() {
        for (String role : List.of("USER", "ADMIN")) {
            UserInfoDTO response = UserInfoDTO.of(user(role, null, "NAVER").getMember());
            assertNull(response.getPlaceId());
            assertEquals(List.of("ROLE_" + role), response.getRoles());
            JsonNode json = objectMapper.valueToTree(response);
            assertTrue(json.has("placeId"));
            assertTrue(json.get("placeId").isNull());
        }
    }

    private CustomUser user(String role, Long placeId, String provider) {
        AuthVO authority = new AuthVO();
        authority.setAuth("ROLE_" + role);
        return new CustomUser(MemberVO.builder()
                .id(2L)
                .username("owner@example.com")
                .email("owner@example.com")
                .password("unused")
                .nickname("매장주")
                .provider(provider)
                .role(role)
                .placeId(placeId)
                .authList(List.of(authority))
                .build());
    }
}
