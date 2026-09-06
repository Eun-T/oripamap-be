package org.scoula.security.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtCookieUtilTest {

    @Test
    void addAccessTokenCookieUsesSecureInHttpsEnvironment() {
        JwtCookieUtil cookieUtil = new JwtCookieUtil(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addAccessTokenCookie(response, "jwt-value", Duration.ofMinutes(30));

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("accessToken=jwt-value"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=1800"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Secure"));
    }

    @Test
    void addAccessTokenCookieOmitsSecureInLocalHttpEnvironment() {
        JwtCookieUtil cookieUtil = new JwtCookieUtil(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addAccessTokenCookie(response, "jwt-value");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertFalse(cookie.contains("Secure"));
    }

    @Test
    void deleteAccessTokenCookieKeepsIssuingSecurityAttributes() {
        JwtCookieUtil cookieUtil = new JwtCookieUtil(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.deleteAccessTokenCookie(response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.contains("accessToken="));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Secure"));
    }
}
