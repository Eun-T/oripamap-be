package org.scoula.member.service;

import org.scoula.emailverification.domain.EmailVerification;
import org.scoula.emailverification.mapper.EmailVerificationMapper;

import java.time.LocalDateTime;

class VerifiedEmailVerificationMapper implements EmailVerificationMapper {
    @Override
    public int insertIfAbsent(String email, String codeHash, LocalDateTime expiresAt,
                              LocalDateTime lastSentAt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EmailVerification findByEmailForUpdate(String email) {
        EmailVerification verification = new EmailVerification();
        verification.setId(1L);
        verification.setEmail(email);
        verification.setVerifiedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        return verification;
    }

    @Override
    public int replaceCode(Long id, String codeHash, LocalDateTime expiresAt,
                           LocalDateTime lastSentAt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int incrementAttemptCount(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int markVerified(Long id, LocalDateTime verifiedAt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int deleteById(Long id) {
        return 1;
    }
}
