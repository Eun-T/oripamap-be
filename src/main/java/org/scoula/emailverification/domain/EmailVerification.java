package org.scoula.emailverification.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EmailVerification {
    private Long id;
    private String email;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private int attemptCount;
    private LocalDateTime lastSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
