package org.scoula.security.handler;

import lombok.RequiredArgsConstructor;
import org.scoula.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.security.service.LoginRateLimiter;
import org.scoula.security.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private final LoginRateLimiter loginRateLimiter;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = (String) request.getAttribute(
                JwtUsernamePasswordAuthenticationFilter.LOGIN_EMAIL_ATTRIBUTE);
        boolean blocked = exception instanceof LockedException
                || loginRateLimiter.recordFailure(request, email);
        if (blocked) {
            response.setHeader("Retry-After", "600");
            JsonResponse.sendError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "\uB85C\uADF8\uC778 \uC2DC\uB3C4\uAC00 \uB108\uBB34 \uB9CE\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.");
            return;
        }
        JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, "사용자 ID 또는 비밀번호가 일치하지 않습니다.");
    }
}
