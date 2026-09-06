package org.scoula.security.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

public final class OAuthStateCookieUtil {

    private static final Duration STATE_MAX_AGE = Duration.ofMinutes(5);

    private OAuthStateCookieUtil() {
    }

    public static void addStateCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path,
            boolean secure) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookie(name, value, path, STATE_MAX_AGE, secure).toString());
    }

    public static void deleteStateCookie(
            HttpServletResponse response,
            String name,
            String path,
            boolean secure) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookie(name, "", path, Duration.ZERO, secure).toString());
    }

    private static ResponseCookie createCookie(
            String name,
            String value,
            String path,
            Duration maxAge,
            boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
