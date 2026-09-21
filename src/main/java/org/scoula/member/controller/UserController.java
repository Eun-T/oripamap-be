package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.member.dto.NicknameUpdateDTO;
import org.scoula.member.service.MemberService;
import org.scoula.member.service.MemberWithdrawalService;
import org.scoula.member.util.NicknamePolicy;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.UserInfoDTO;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.RefreshTokenCookieUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final MemberService memberService;
    private final MemberWithdrawalService memberWithdrawalService;
    private final JwtCookieUtil jwtCookieUtil;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;

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

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUser user,
            HttpServletRequest request,
            HttpServletResponse response) {
        memberWithdrawalService.withdraw(user.getMember().getId());
        jwtCookieUtil.deleteAccessTokenCookie(request, response);
        refreshTokenCookieUtil.deleteRefreshTokenCookie(request, response);
        return ResponseEntity.noContent().build();
    }
}
