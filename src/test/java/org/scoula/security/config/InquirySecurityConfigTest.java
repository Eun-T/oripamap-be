package org.scoula.security.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.Filter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RootConfig.class,
        SecurityConfig.class
})
class InquirySecurityConfigTest {

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedInquiryRequestReturnsUnauthorizedBeforeController() throws Exception {
        FilterResult result = perform("GET", "/api/inquiries");

        assertEquals(401, result.status());
        assertFalse(result.controllerReached());
    }

    @Test
    void authenticatedUserCanAccessInquiryApi() throws Exception {
        authenticateAs("ROLE_USER");

        boolean controllerReached = performAuthorization("POST", "/api/inquiries");

        assertTrue(controllerReached);
    }

    @Test
    void nonAdminCannotAccessAdminInquiryApi() {
        authenticateAs("ROLE_USER");

        assertThrows(AccessDeniedException.class,
                () -> performAuthorization("PUT", "/api/admin/inquiries/1/answer"));
    }

    @Test
    void adminCanAccessAdminInquiryApi() throws Exception {
        authenticateAs("ROLE_ADMIN");

        boolean controllerReached = performAuthorization("PUT", "/api/admin/inquiries/1/answer");

        assertTrue(controllerReached);
    }

    private void authenticateAs(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test-user",
                        null,
                        List.of(new SimpleGrantedAuthority(authority)))
        );
    }

    private FilterResult perform(String method, String path) throws Exception {
        MockHttpServletRequest request = request(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean controllerReached = new AtomicBoolean(false);

        securityFilterChain.doFilter(request, response,
                (servletRequest, servletResponse) -> controllerReached.set(true));

        return new FilterResult(response.getStatus(), controllerReached.get());
    }

    private boolean performAuthorization(String method, String path) throws Exception {
        MockHttpServletRequest request = request(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean controllerReached = new AtomicBoolean(false);

        FilterSecurityInterceptor authorizationFilter = ((FilterChainProxy) securityFilterChain)
                .getFilters(path).stream()
                .filter(FilterSecurityInterceptor.class::isInstance)
                .map(FilterSecurityInterceptor.class::cast)
                .findFirst()
                .orElseThrow();

        authorizationFilter.doFilter(request, response,
                (servletRequest, servletResponse) -> controllerReached.set(true));

        return controllerReached.get();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private record FilterResult(int status, boolean controllerReached) {
    }
}
