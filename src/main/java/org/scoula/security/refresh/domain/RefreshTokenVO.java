package org.scoula.security.refresh.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenVO {
    private Long id;
    private Long userId;
    private String tokenHash;
    private Date expiresAt;
    private Date createdAt;
    private Date revokedAt;
}
