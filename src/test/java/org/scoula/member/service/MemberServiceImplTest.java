package org.scoula.member.service;

import org.junit.jupiter.api.Test;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.exception.NicknameAlreadyExistsException;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberServiceImplTest {

    @Test
    void translatesConcurrentDuplicateEmailDuringJoin() {
        MemberMapper mapper = new DuplicateEmailMemberMapper();
        MemberService service = new MemberServiceImpl(new BCryptPasswordEncoder(), mapper, null);
        MemberJoinDTO request = MemberJoinDTO.builder()
                .email("member@example.com")
                .password("password")
                .nickname("member")
                .build();

        assertThrows(EmailAlreadyExistsException.class, () -> service.join(request));
    }

    @Test
    void translatesConcurrentDuplicateNicknameDuringJoin() {
        ConcurrentNicknameMemberMapper mapper = new ConcurrentNicknameMemberMapper();
        MemberService service = new MemberServiceImpl(new BCryptPasswordEncoder(), mapper, null);
        MemberJoinDTO request = MemberJoinDTO.builder()
                .email("member@example.com")
                .password("password")
                .nickname("닉네임")
                .build();

        assertThrows(NicknameAlreadyExistsException.class, () -> service.join(request));
    }

    @Test
    void translatesConcurrentDuplicateNicknameDuringJoin() {
        ConcurrentNicknameMemberMapper mapper = new ConcurrentNicknameMemberMapper();
        MemberService service = new MemberServiceImpl(new BCryptPasswordEncoder(), mapper, null);
        MemberJoinDTO request = MemberJoinDTO.builder()
                .email("member@example.com")
                .password("password")
                .nickname("닉네임")
                .build();

        assertThrows(NicknameAlreadyExistsException.class, () -> service.join(request));
    }

    @Test
    void rejectsNicknameUsedByAnotherUserBeforeUpdate() {
        NicknameMemberMapper mapper = new NicknameMemberMapper();
        mapper.nicknameExistsForOtherUser = true;
        MemberService service = new MemberServiceImpl(new BCryptPasswordEncoder(), mapper, null);

        assertThrows(NicknameAlreadyExistsException.class,
                () -> service.updateNickname(1L, "중복닉네임"));
        assertEquals(0, mapper.updateCount);
    }

    @Test
    void allowsCurrentUsersOwnNickname() {
        NicknameMemberMapper mapper = new NicknameMemberMapper();
        MemberService service = new MemberServiceImpl(new BCryptPasswordEncoder(), mapper, null);

        service.updateNickname(1L, "내닉네임");

        assertEquals(1, mapper.updateCount);
    }

    @Test
    void translatesUniqueConstraintViolationDuringNicknameUpdate() {
        NicknameMemberMapper mapper = new NicknameMemberMapper();
        mapper.updateException = new DuplicateKeyException("uk_users_nickname");
        MemberService service = new MemberServiceImpl(new BCryptPasswordEncoder(), mapper, null);

        assertThrows(NicknameAlreadyExistsException.class,
                () -> service.updateNickname(1L, "경합닉네임"));
    }

    private static class DuplicateEmailMemberMapper implements MemberMapper {
        @Override
        public MemberVO get(String email) {
            return null;
        }

        @Override
        public boolean existsByEmail(String email) {
            return true;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return false;
        }

        @Override
        public boolean existsByNicknameExcludingUser(String nickname, Long userId) {
            return false;
        }

        @Override
        public int insert(MemberVO member) {
            throw new DuplicateKeyException("duplicate email");
        }

        @Override
        public int update(MemberVO member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateNickname(Long id, String nickname) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updatePassword(ChangePasswordDTO changePasswordDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberVO findByProvider(String provider, String providerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int insertSocial(MemberVO member) {
            throw new UnsupportedOperationException();
        }
    }

    private static class NicknameMemberMapper extends DuplicateEmailMemberMapper {
        private boolean nicknameExistsForOtherUser;
        private DuplicateKeyException updateException;
        private int updateCount;

        @Override
        public boolean existsByNicknameExcludingUser(String nickname, Long userId) {
            return nicknameExistsForOtherUser;
        }

        @Override
        public int updateNickname(Long id, String nickname) {
            if (updateException != null) {
                throw updateException;
            }
            updateCount++;
            return 1;
        }
    }

    private static class ConcurrentNicknameMemberMapper extends DuplicateEmailMemberMapper {
        private int nicknameChecks;

        @Override
        public boolean existsByEmail(String email) {
            return false;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return nicknameChecks++ > 0;
        }

        @Override
        public int insert(MemberVO member) {
            throw new DuplicateKeyException("uk_users_nickname");
        }
    }

    private static class ConcurrentNicknameMemberMapper extends DuplicateEmailMemberMapper {
        private int nicknameChecks;

        @Override
        public boolean existsByEmail(String email) {
            return false;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return nicknameChecks++ > 0;
        }

        @Override
        public int insert(MemberVO member) {
            throw new DuplicateKeyException("uk_users_nickname");
        }
    }
}
