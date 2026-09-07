package org.scoula.security.refresh.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.security.refresh.domain.RefreshTokenVO;

import java.util.Date;

public interface RefreshTokenMapper {
    int insert(RefreshTokenVO refreshToken);
    RefreshTokenVO findByTokenHash(@Param("tokenHash") String tokenHash);
    int revokeByTokenHash(@Param("tokenHash") String tokenHash,
                          @Param("revokedAt") Date revokedAt);
    int revokeAllByUserId(@Param("userId") Long userId,
                          @Param("revokedAt") Date revokedAt);
}
