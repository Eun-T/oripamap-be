package org.scoula.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationErrorFilterTest {

    @Test
    void returnsUnauthorizedWhenJwtReferencesDeletedUser() throws Exception {
        AuthenticationErrorFilter filter = new AuthenticationErrorFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response,
                (request, chainResponse) -> {
                    throw new UsernameNotFoundException("deleted user");
                });

        assertEquals(401, response.getStatus());
    }
}
