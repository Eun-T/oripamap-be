package org.scoula.member.service;

import org.junit.jupiter.api.Test;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
        public int insert(MemberVO member) {
            throw new DuplicateKeyException("duplicate email");
        }

        @Override
        public int update(MemberVO member) {
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
}
