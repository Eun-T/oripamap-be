package org.scoula.emailverification.service;

import lombok.RequiredArgsConstructor;
import org.scoula.emailverification.domain.EmailVerification;
import org.scoula.emailverification.exception.EmailVerificationRateLimitException;
import org.scoula.emailverification.mapper.EmailVerificationMapper;
import org.scoula.emailverification.util.EmailAddressUtil;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.mapper.MemberMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final int CODE_BOUND = 1_000_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long RESEND_INTERVAL_SECONDS = 60;
    private static final long EXPIRY_MINUTES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationMapper verificationMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailClient resendEmailClient;

    @Transactional
    public void send(String rawEmail) {
        String email = validatedEmail(rawEmail);
        if (memberMapper.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        String code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(CODE_BOUND));
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRY_MINUTES);

        int inserted = verificationMapper.insertIfAbsent(email, codeHash, expiresAt, now);
        if (inserted == 0) {
            EmailVerification current = verificationMapper.findByEmailForUpdate(email);
            enforceResendInterval(current, now);
            verificationMapper.replaceCode(current.getId(), codeHash, expiresAt, now);
        }

        resendEmailClient.sendVerificationCode(email, code);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void verify(String rawEmail, String code) {
        String email = validatedEmail(rawEmail);
        if (code == null || !code.matches("\\d{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호는 6자리 숫자여야 합니다.");
        }

        EmailVerification current = verificationMapper.findByEmailForUpdate(email);
        if (current == null) {
            throw invalidVerification();
        }
        if (current.getVerifiedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 인증이 완료되었습니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!current.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "인증번호가 만료되었습니다.");
        }
        if (current.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호 입력 가능 횟수를 초과했습니다.");
        }
        if (!passwordEncoder.matches(code, current.getCodeHash())) {
            verificationMapper.incrementAttemptCount(current.getId());
            throw invalidVerification();
        }

        verificationMapper.markVerified(current.getId(), now);
    }

    private String validatedEmail(String rawEmail) {
        String email = EmailAddressUtil.normalize(rawEmail);
        if (!EmailAddressUtil.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.");
        }
        return email;
    }

    private void enforceResendInterval(EmailVerification current, LocalDateTime now) {
        long elapsed = Duration.between(current.getLastSentAt(), now).getSeconds();
        if (elapsed < RESEND_INTERVAL_SECONDS) {
            long retryAfter = Math.max(1, RESEND_INTERVAL_SECONDS - elapsed);
            throw new EmailVerificationRateLimitException(retryAfter);
        }
    }

    private ResponseStatusException invalidVerification() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 또는 인증번호가 올바르지 않습니다.");
    }
}
