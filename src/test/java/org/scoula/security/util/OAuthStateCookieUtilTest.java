package org.scoula.security.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthStateCookieUtilTest {

    @Test
    void addStateCookieUsesSecureInHttpsEnvironment() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuthStateCookieUtil.addStateCookie(
                request,
                response,
                "kakao_oauth_state",
                "state-value",
                "/api/auth/kakao",
                true);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("kakao_oauth_state=state-value"));
        assertTrue(cookie.contains("Path=/api/auth/kakao"));
        assertTrue(cookie.contains("Max-Age=300"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Secure"));
    }

    @Test
    void addStateCookieOmitsSecureInLocalHttpEnvironment() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuthStateCookieUtil.addStateCookie(
                request,
                response,
                "naver_oauth_state",
                "state-value",
                "/api/auth/naver",
                false);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertFalse(cookie.contains("Secure"));
    }

    @Test
    void addStateCookieUsesCrossSitePolicyForAppRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ClientRequestUtil.CLIENT_TYPE_HEADER, "APP");
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuthStateCookieUtil.addStateCookie(
                request,
                response,
                "kakao_oauth_state",
                "state-value",
                "/api/auth/kakao",
                false);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));
    }

    @Test
    void deleteStateCookieKeepsSameSecurityAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuthStateCookieUtil.deleteStateCookie(
                request,
                response,
                "kakao_oauth_state",
                "/api/auth/kakao",
                true);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("kakao_oauth_state="));
        assertTrue(cookie.contains("Path=/api/auth/kakao"));
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Secure"));
    }
}
