package org.scoula.security.handler;

import lombok.RequiredArgsConstructor;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.UserInfoDTO;
import org.scoula.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.security.refresh.service.RefreshTokenService;
import org.scoula.security.service.LoginRateLimiter;
import org.scoula.security.util.JsonResponse;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.JwtProcessor;
import org.scoula.security.util.RefreshTokenCookieUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProcessor jwtProcessor;
    private final JwtCookieUtil jwtCookieUtil;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;
    private final LoginRateLimiter loginRateLimiter;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUser user = (CustomUser) authentication.getPrincipal();
        loginRateLimiter.reset(request, (String) request.getAttribute(
                JwtUsernamePasswordAuthenticationFilter.LOGIN_EMAIL_ATTRIBUTE));
        Long userId = user.getMember().getId();
        String accessToken = jwtProcessor.generateToken(userId);
        String refreshToken = refreshTokenService.issue(userId);
        jwtCookieUtil.addAccessTokenCookie(response, accessToken);
        refreshTokenCookieUtil.addRefreshTokenCookie(response, refreshToken);
        JsonResponse.send(response, UserInfoDTO.of(user.getMember()));
    }
}
