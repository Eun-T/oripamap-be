package org.scoula.security.config;

import org.junit.jupiter.api.Test;
import org.scoula.security.util.ClientRequestUtil;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppAuthCsrfRequestMatcherTest {
    private final AppAuthCsrfRequestMatcher matcher = new AppAuthCsrfRequestMatcher();

    @Test
    void matchesOnlyAppPostForLogoutOrRefresh() {
        assertTrue(matcher.matches(request("POST", "/api/auth/logout", "APP")));
        assertTrue(matcher.matches(request("POST", "/api/auth/refresh", "APP")));

        assertFalse(matcher.matches(request("POST", "/api/auth/logout", null)));
        assertFalse(matcher.matches(request("POST", "/api/auth/refresh", "WEB")));
        assertFalse(matcher.matches(request("GET", "/api/auth/logout", "APP")));
        assertFalse(matcher.matches(request("POST", "/api/auth/login", "APP")));
    }

    @Test
    void matchesPathAfterRemovingContextPath() {
        MockHttpServletRequest request = request(
                "POST", "/oripa/api/auth/logout", "APP");
        request.setContextPath("/oripa");

        assertTrue(matcher.matches(request));
    }

    private MockHttpServletRequest request(String method, String uri, String clientType) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (clientType != null) {
            request.addHeader(ClientRequestUtil.CLIENT_TYPE_HEADER, clientType);
        }
        return request;
    }
}
