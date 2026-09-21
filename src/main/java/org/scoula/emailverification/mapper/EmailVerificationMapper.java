package org.scoula.emailverification.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.emailverification.domain.EmailVerification;

import java.time.LocalDateTime;

public interface EmailVerificationMapper {
    int insertIfAbsent(@Param("email") String email,
                       @Param("codeHash") String codeHash,
                       @Param("expiresAt") LocalDateTime expiresAt,
                       @Param("lastSentAt") LocalDateTime lastSentAt);

    EmailVerification findByEmailForUpdate(@Param("email") String email);

    int replaceCode(@Param("id") Long id,
                    @Param("codeHash") String codeHash,
                    @Param("expiresAt") LocalDateTime expiresAt,
                    @Param("lastSentAt") LocalDateTime lastSentAt);

    int incrementAttemptCount(@Param("id") Long id);

    int markVerified(@Param("id") Long id, @Param("verifiedAt") LocalDateTime verifiedAt);

    int deleteById(@Param("id") Long id);
}
