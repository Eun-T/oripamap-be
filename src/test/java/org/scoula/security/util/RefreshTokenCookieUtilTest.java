package org.scoula.security.util;

import org.junit.jupiter.api.Test;
import org.scoula.security.refresh.service.RefreshTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCookieUtilTest {

    @Test
    void addRefreshTokenCookieKeepsWebPolicyAndValidity() {
        RefreshTokenCookieUtil cookieUtil = new RefreshTokenCookieUtil(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addRefreshTokenCookie(request, response, "refresh-value");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("refreshToken=refresh-value"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=" + RefreshTokenService.VALIDITY.getSeconds()));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertFalse(cookie.contains("Secure"));
    }

    @Test
    void addRefreshTokenCookieUsesCrossSitePolicyForAppRequest() {
        RefreshTokenCookieUtil cookieUtil = new RefreshTokenCookieUtil(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ClientRequestUtil.CLIENT_TYPE_HEADER, "APP");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addRefreshTokenCookie(request, response, "refresh-value");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));
    }

    @Test
    void deleteRefreshTokenCookieUsesAppSecurityAttributes() {
        RefreshTokenCookieUtil cookieUtil = new RefreshTokenCookieUtil(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ClientRequestUtil.CLIENT_TYPE_HEADER, "APP");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.deleteRefreshTokenCookie(request, response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));
    }
}
