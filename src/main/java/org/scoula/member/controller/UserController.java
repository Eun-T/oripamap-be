package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.member.dto.NicknameUpdateDTO;
import org.scoula.member.service.MemberService;
import org.scoula.member.util.NicknamePolicy;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.UserInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoDTO> getMe(@AuthenticationPrincipal CustomUser user) {
        return ResponseEntity.ok(UserInfoDTO.of(user.getMember()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserInfoDTO> updateNickname(
            @RequestBody NicknameUpdateDTO request,
            @AuthenticationPrincipal CustomUser user) {
        String nickname = request == null ? null : request.getNickname();
        if (!NicknamePolicy.isValid(nickname)) {
            return ResponseEntity.badRequest().build();
        }

        memberService.updateNickname(user.getMember().getId(), nickname);
        user.getMember().setNickname(nickname);
        return ResponseEntity.ok(UserInfoDTO.of(user.getMember()));
    }
}
