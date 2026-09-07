package org.scoula.security.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Component
public final class JwtCookieUtil {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final Duration ACCESS_TOKEN_MAX_AGE = Duration.ofMinutes(30);

    private final boolean secure;

    public JwtCookieUtil(@Value("${jwt.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie(token, ACCESS_TOKEN_MAX_AGE).toString());
    }

    void addAccessTokenCookie(HttpServletResponse response, String token, Duration maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie(token, maxAge).toString());
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie createCookie(String value, Duration maxAge) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
