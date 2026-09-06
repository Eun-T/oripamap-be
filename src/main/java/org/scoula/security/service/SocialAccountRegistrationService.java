package org.scoula.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class SocialAccountRegistrationService {

    private final MemberMapper memberMapper;

    public MemberVO insertOrGetExisting(MemberVO newMember) {
        try {
            memberMapper.insertSocial(newMember);
        } catch (DuplicateKeyException e) {
            MemberVO existingMember = findSameSocialAccount(newMember);
            if (existingMember == null) {
                throw e;
            }

            log.info("동시 {} 소셜 가입 요청에서 기존 회원을 사용합니다.", newMember.getProvider());
            return existingMember;
        }

        MemberVO insertedMember = findSameSocialAccount(newMember);
        if (insertedMember == null) {
            throw new IllegalStateException("등록한 소셜 회원을 다시 조회할 수 없습니다.");
        }
        return insertedMember;
    }

    private MemberVO findSameSocialAccount(MemberVO member) {
        return memberMapper.findByProvider(member.getProvider(), member.getProviderId());
    }
}
