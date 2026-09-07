package org.scoula.security.util;

import org.scoula.security.refresh.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Component
public final class RefreshTokenCookieUtil {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final boolean secure;

    public RefreshTokenCookieUtil(@Value("${jwt.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                createCookie(token, RefreshTokenService.VALIDITY).toString());
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                createCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie createCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
