package org.scoula.member.controller;

import org.junit.jupiter.api.Test;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.dto.NicknameUpdateDTO;
import org.scoula.member.service.MemberService;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.account.dto.UserInfoDTO;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {

    @Test
    void updatesOnlyAuthenticatedUsersNicknameAndReturnsChangedUserInfo() {
        TrackingMemberService service = new TrackingMemberService();
        UserController controller = new UserController(service);
        CustomUser user = authenticatedUser();

        UserInfoDTO body = controller.updateNickname(
                new NicknameUpdateDTO("새 닉네임"), user).getBody();

        assertTrue(service.updateNicknameCalled);
        assertEquals(7L, service.userId);
        assertEquals("새 닉네임", service.nickname);
        assertEquals("새 닉네임", body.getNickname());
        assertEquals("member@example.com", body.getEmail());
        assertEquals("NAVER", body.getProvider());
    }

    @Test
    void rejectsBlankNicknameWithoutUpdating() {
        TrackingMemberService service = new TrackingMemberService();
        UserController controller = new UserController(service);

        var response = controller.updateNickname(new NicknameUpdateDTO("   "), authenticatedUser());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(service.updateNicknameCalled);
    }

    @Test
    void rejectsNicknameLongerThanSevenCharactersWithoutUpdating() {
        TrackingMemberService service = new TrackingMemberService();
        UserController controller = new UserController(service);

        var response = controller.updateNickname(
                new NicknameUpdateDTO("여덟글자닉네임임"), authenticatedUser());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(service.updateNicknameCalled);
    }

    @Test
    void rejectsNicknameContainingWhitespaceWithoutUpdating() {
        TrackingMemberService service = new TrackingMemberService();
        UserController controller = new UserController(service);

        var response = controller.updateNickname(
                new NicknameUpdateDTO("공백 닉네임"), authenticatedUser());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(service.updateNicknameCalled);
    }

    private CustomUser authenticatedUser() {
        return new CustomUser(MemberVO.builder()
                .id(7L)
                .username("member@example.com")
                .email("member@example.com")
                .password("unused")
                .nickname("기존 닉네임")
                .provider("NAVER")
                .authList(Collections.emptyList())
                .build());
    }

    private static class TrackingMemberService implements MemberService {
        private boolean updateNicknameCalled;
        private Long userId;
        private String nickname;

        @Override
        public void updateNickname(Long userId, String nickname) {
            updateNicknameCalled = true;
            this.userId = userId;
            this.nickname = nickname;
        }

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
            throw new UnsupportedOperationException();
        }

        @Override
        public void changePassword(String authenticatedUsername, ChangePasswordDTO changePassword) {
            throw new UnsupportedOperationException();
        }
    }
}
