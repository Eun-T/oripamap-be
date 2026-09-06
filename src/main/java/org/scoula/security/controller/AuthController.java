package org.scoula.security.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.security.util.JwtCookieUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtCookieUtil jwtCookieUtil;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        jwtCookieUtil.deleteAccessTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
