package org.scoula.member.controller;

import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.UserInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserInfoDTO> getMe(@AuthenticationPrincipal CustomUser user) {
        return ResponseEntity.ok(UserInfoDTO.of(user.getMember()));
    }
}
