package org.scoula.security.refresh.service;

import lombok.RequiredArgsConstructor;
import org.scoula.security.refresh.domain.RefreshTokenVO;
import org.scoula.security.refresh.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    public static final Duration VALIDITY = Duration.ofDays(7);
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenMapper mapper;

    @Transactional
    public String issue(Long userId) {
        return createAndStore(userId);
    }

    @Transactional
    public Optional<RotationResult> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = hash(rawToken);
        RefreshTokenVO stored = mapper.findByTokenHash(tokenHash);
        Date now = new Date();
        if (stored == null || stored.getRevokedAt() != null
                || !stored.getExpiresAt().after(now)) {
            return Optional.empty();
        }
        if (mapper.revokeByTokenHash(tokenHash, now) != 1) {
            return Optional.empty();
        }

        String replacement = createAndStore(stored.getUserId());
        return Optional.of(new RotationResult(stored.getUserId(), replacement));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            mapper.revokeByTokenHash(hash(rawToken), new Date());
        }
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        mapper.revokeAllByUserId(userId, new Date());
    }

    private String createAndStore(Long userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        mapper.insert(RefreshTokenVO.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(Date.from(Instant.now().plus(VALIDITY)))
                .build());
        return rawToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    public record RotationResult(Long userId, String refreshToken) {
    }
}
