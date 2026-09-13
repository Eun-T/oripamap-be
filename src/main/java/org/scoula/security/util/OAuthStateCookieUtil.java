package org.scoula.security.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

public final class OAuthStateCookieUtil {

    private static final Duration STATE_MAX_AGE = Duration.ofMinutes(5);

    private OAuthStateCookieUtil() {
    }

    public static void addStateCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String name,
            String value,
            String path,
            boolean secure) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookie(request, name, value, path, STATE_MAX_AGE, secure).toString());
    }

    public static void deleteStateCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String name,
            String path,
            boolean secure) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookie(request, name, "", path, Duration.ZERO, secure).toString());
    }

    private static ResponseCookie createCookie(
            HttpServletRequest request,
            String name,
            String value,
            String path,
            Duration maxAge,
            boolean secure) {
        boolean appClient = ClientRequestUtil.isApp(request);
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(appClient || secure)
                .sameSite(appClient ? "None" : "Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
