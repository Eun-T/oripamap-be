package org.scoula.security.handler;

import lombok.RequiredArgsConstructor;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.UserInfoDTO;
import org.scoula.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.security.util.JsonResponse;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.JwtProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Duration DEFAULT_LOGIN_DURATION = Duration.ofMinutes(30);
    private static final Duration REMEMBER_ME_DURATION = Duration.ofDays(7);

    private final JwtProcessor jwtProcessor;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUser user = (CustomUser) authentication.getPrincipal();
        boolean rememberMe = Boolean.TRUE.equals(request.getAttribute(
                JwtUsernamePasswordAuthenticationFilter.REMEMBER_ME_ATTRIBUTE));
        Duration loginDuration = rememberMe ? REMEMBER_ME_DURATION : DEFAULT_LOGIN_DURATION;
        String token = jwtProcessor.generateToken(user.getMember().getId(), loginDuration);
        JwtCookieUtil.addAccessTokenCookie(response, token, loginDuration);
        JsonResponse.send(response, UserInfoDTO.of(user.getMember()));
    }
}
