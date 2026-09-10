package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.service.MemberService;
import org.scoula.member.util.NicknamePolicy;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Log4j2
@RestController //json, string으로 리턴
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
    final MemberService service;

    @Deprecated
    @GetMapping("/checkusername/{email}")
    public ResponseEntity<Boolean> checkEmailCompatibilityAlias(@PathVariable("email") String email) {
        return ResponseEntity.ok(service.existsByEmail(email));
    }
    @GetMapping("/checkemail/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(service.existsByEmail(email));
    }
    @PostMapping("")
    public ResponseEntity<MemberDTO> join(MemberJoinDTO member) {
        String nickname = member.getNickname() == null || member.getNickname().isBlank()
                ? member.getUsername()
                : member.getNickname();
        if (member.getEmail() == null || member.getEmail().isBlank()
                || member.getPassword() == null || member.getPassword().isBlank()
                || !NicknamePolicy.isValid(nickname)) {
            return ResponseEntity.badRequest().build();
        }
        if (service.existsByEmail(member.getEmail())) {
            throw new EmailAlreadyExistsException();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.join(member));
    }

    @PutMapping("/{username}")
    public ResponseEntity<MemberDTO> changeProfile(
            @PathVariable("username") String username,
            MemberUpdateDTO member,
            @AuthenticationPrincipal CustomUser user) {
        validateOwner(username, user);
        return ResponseEntity.ok(service.update(user.getUsername(), member));
    }

    @PutMapping("/{username}/changepassword")
    public ResponseEntity<?> changePassword(
            @PathVariable("username") String username,
            @RequestBody ChangePasswordDTO changePasswordDTO,
            @AuthenticationPrincipal CustomUser user) {
        validateOwner(username, user);
        service.changePassword(user.getUsername(), changePasswordDTO);
        return ResponseEntity.ok().build();
    }

    private void validateOwner(String username, CustomUser user) {
        if (!user.getUsername().equals(username)) {
            throw new AccessDeniedException("다른 회원의 정보를 변경할 수 없습니다.");
        }
    }

}
