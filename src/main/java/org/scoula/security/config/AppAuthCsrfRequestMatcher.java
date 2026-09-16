package org.scoula.security.config;

import org.scoula.security.util.ClientRequestUtil;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

public final class AppAuthCsrfRequestMatcher implements RequestMatcher {
    private static final Set<String> CSRF_EXCLUDED_PATHS = Set.of(
            "/api/auth/logout",
            "/api/auth/refresh");

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        return CSRF_EXCLUDED_PATHS.contains(path) && ClientRequestUtil.isApp(request);
    }
}
