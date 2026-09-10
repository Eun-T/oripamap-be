package org.scoula.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.exception.NicknameAlreadyExistsException;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.member.util.NicknamePolicy;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class SocialAccountRegistrationService {
    private static final int MAX_NICKNAME_ATTEMPTS = 20;

    private final MemberMapper memberMapper;

    public MemberVO insertOrGetExisting(MemberVO newMember) {
        String baseNickname = NicknamePolicy.normalizeGenerated(
                newMember.getNickname(),
                "소셜회원");
        newMember.setNickname(baseNickname);

        for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
            if (attempt > 0 || memberMapper.existsByNickname(newMember.getNickname())) {
                newMember.setNickname(nicknameCandidate(baseNickname, newMember, attempt));
            }

            try {
                memberMapper.insertSocial(newMember);
            } catch (DuplicateKeyException e) {
                MemberVO existingMember = findSameSocialAccount(newMember);
                if (existingMember != null) {
                    log.info("동시 {} 소셜 가입 요청에서 기존 회원을 사용합니다.", newMember.getProvider());
                    return existingMember;
                }
                if (memberMapper.existsByEmail(newMember.getEmail())) {
                    throw new EmailAlreadyExistsException(e);
                }
                if (memberMapper.existsByNickname(newMember.getNickname())) {
                    continue;
                }
                throw e;
            }

            MemberVO insertedMember = findSameSocialAccount(newMember);
            if (insertedMember == null) {
                throw new IllegalStateException("등록한 소셜 회원을 다시 조회할 수 없습니다.");
            }
            return insertedMember;
        }

        throw new NicknameAlreadyExistsException();
    }

    private String nicknameCandidate(String baseNickname, MemberVO member, int attempt) {
        int hash = java.util.Objects.hash(member.getProvider(), member.getProviderId(), attempt);
        String encoded = Integer.toUnsignedString(hash, 36);
        String suffix = encoded.substring(Math.max(0, encoded.length() - 4));
        return NicknamePolicy.withSuffix(baseNickname, suffix);
    }

    private MemberVO findSameSocialAccount(MemberVO member) {
        return memberMapper.findByProvider(member.getProvider(), member.getProviderId());
    }
}
