package org.scoula.security.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.security.refresh.service.RefreshTokenService;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.JwtProcessor;
import org.scoula.security.util.RefreshTokenCookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtCookieUtil jwtCookieUtil;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;
    private final RefreshTokenService refreshTokenService;
    private final JwtProcessor jwtProcessor;

    // TODO: refresh/logout use cookies and must be protected when CSRF is enabled.
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = RefreshTokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME,
                    required = false) String refreshToken,
            HttpServletResponse response) {
        return refreshTokenService.rotate(refreshToken)
                .map(rotated -> {
                    String accessToken = jwtProcessor.generateToken(rotated.userId());
                    jwtCookieUtil.addAccessTokenCookie(response, accessToken);
                    refreshTokenCookieUtil.addRefreshTokenCookie(
                            response, rotated.refreshToken());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> {
                    jwtCookieUtil.deleteAccessTokenCookie(response);
                    refreshTokenCookieUtil.deleteRefreshTokenCookie(response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                });
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = RefreshTokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME,
                    required = false) String refreshToken,
            HttpServletResponse response) {
        refreshTokenService.revoke(refreshToken);
        jwtCookieUtil.deleteAccessTokenCookie(response);
        refreshTokenCookieUtil.deleteRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
