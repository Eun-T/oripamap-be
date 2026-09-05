package org.scoula.security.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

public final class JwtCookieUtil {
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final Duration ACCESS_TOKEN_MAX_AGE = Duration.ofMinutes(30);
    private JwtCookieUtil() { }

    public static void addAccessTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie(token, ACCESS_TOKEN_MAX_AGE).toString());
    }

    public static void addAccessTokenCookie(HttpServletResponse response, String token, Duration maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie(token, maxAge).toString());
    }

    public static void deleteAccessTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("", Duration.ZERO).toString());
    }

    private static ResponseCookie createCookie(String value, Duration maxAge) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, value).httpOnly(true).secure(false)
                .sameSite("Lax").path("/").maxAge(maxAge).build();
    }
}
