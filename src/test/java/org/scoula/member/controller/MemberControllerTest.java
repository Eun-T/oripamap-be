package org.scoula.member.controller;

import org.junit.jupiter.api.Test;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.service.MemberService;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberControllerTest {

    @Test
    void rejectsProfileChangeForDifferentPathUsername() {
        TrackingMemberService service = new TrackingMemberService();
        MemberController controller = new MemberController(service);

        assertThrows(
                AccessDeniedException.class,
                () -> controller.changeProfile(
                        "other@example.com",
                        new MemberUpdateDTO(),
                        authenticatedUser("owner@example.com")));

        assertFalse(service.updateCalled);
    }

    @Test
    void rejectsPasswordChangeForDifferentPathUsername() {
        TrackingMemberService service = new TrackingMemberService();
        MemberController controller = new MemberController(service);

        assertThrows(
                AccessDeniedException.class,
                () -> controller.changePassword(
                        "other@example.com",
                        new ChangePasswordDTO(),
                        authenticatedUser("owner@example.com")));

        assertFalse(service.changePasswordCalled);
    }

    @Test
    void passesAuthenticatedUsernameInsteadOfRequestBodyUsername() {
        TrackingMemberService service = new TrackingMemberService();
        MemberController controller = new MemberController(service);
        CustomUser user = authenticatedUser("owner@example.com");

        controller.changeProfile(
                "owner@example.com",
                new MemberUpdateDTO("other@example.com", "password", "new@example.com"),
                user);
        controller.changePassword(
                "owner@example.com",
                new ChangePasswordDTO("other@example.com", "old", "new"),
                user);

        assertTrue(service.updateCalled);
        assertTrue(service.changePasswordCalled);
        assertEquals("owner@example.com", service.updateUsername);
        assertEquals("owner@example.com", service.changePasswordUsername);
    }

    private CustomUser authenticatedUser(String username) {
        return new CustomUser(MemberVO.builder()
                .username(username)
                .password("unused")
                .authList(Collections.emptyList())
                .build());
    }

    private static class TrackingMemberService implements MemberService {
        private boolean updateCalled;
        private boolean changePasswordCalled;
        private String updateUsername;
        private String changePasswordUsername;

        @Override
        public boolean existsByEmail(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberDTO get(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberDTO join(MemberJoinDTO member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberDTO update(String authenticatedUsername, MemberUpdateDTO member) {
            updateCalled = true;
            updateUsername = authenticatedUsername;
            return null;
        }

        @Override
        public void updateNickname(Long userId, String nickname) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void changePassword(String authenticatedUsername, ChangePasswordDTO changePassword) {
            changePasswordCalled = true;
            changePasswordUsername = authenticatedUsername;
        }
    }
}
