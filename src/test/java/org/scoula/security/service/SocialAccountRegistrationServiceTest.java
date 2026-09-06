package org.scoula.security.service;

import org.junit.jupiter.api.Test;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialAccountRegistrationServiceTest {

    @Test
    void returnsExistingMemberWhenSameSocialAccountWasInsertedConcurrently() {
        MemberVO existingMember = socialMember(1L, "KAKAO", "provider-id");
        StubMemberMapper memberMapper = new StubMemberMapper();
        memberMapper.insertException = new DuplicateKeyException("duplicate");
        memberMapper.memberToFind = existingMember;
        SocialAccountRegistrationService service = new SocialAccountRegistrationService(memberMapper);

        MemberVO result = service.insertOrGetExisting(socialMember(null, "KAKAO", "provider-id"));

        assertSame(existingMember, result);
    }

    @Test
    void rethrowsDuplicateKeyWhenSameSocialAccountDoesNotExist() {
        StubMemberMapper memberMapper = new StubMemberMapper();
        memberMapper.insertException = new DuplicateKeyException("unrelated duplicate");
        SocialAccountRegistrationService service = new SocialAccountRegistrationService(memberMapper);

        assertThrows(
                DuplicateKeyException.class,
                () -> service.insertOrGetExisting(socialMember(null, "NAVER", "provider-id")));
    }

    @Test
    void returnsInsertedMemberAfterSuccessfulInsert() {
        MemberVO insertedMember = socialMember(2L, "NAVER", "provider-id");
        StubMemberMapper memberMapper = new StubMemberMapper();
        memberMapper.memberToFind = insertedMember;
        SocialAccountRegistrationService service = new SocialAccountRegistrationService(memberMapper);

        MemberVO result = service.insertOrGetExisting(socialMember(null, "NAVER", "provider-id"));

        assertSame(insertedMember, result);
    }

    private MemberVO socialMember(Long id, String provider, String providerId) {
        return MemberVO.builder()
                .id(id)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    private static class StubMemberMapper implements MemberMapper {
        private DuplicateKeyException insertException;
        private MemberVO memberToFind;

        @Override
        public MemberVO findByProvider(String provider, String providerId) {
            return memberToFind;
        }

        @Override
        public int insertSocial(MemberVO member) {
            if (insertException != null) {
                throw insertException;
            }
            return 1;
        }

        @Override
        public MemberVO get(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemberVO findByUsername(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int insert(MemberVO member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(MemberVO member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updatePassword(ChangePasswordDTO changePasswordDTO) {
            throw new UnsupportedOperationException();
        }
    }
}
