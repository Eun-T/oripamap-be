package org.scoula.security.account.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.security.account.domain.MemberVO;

public interface UserDetailsMapper {
    MemberVO get(@Param("email") String email);
    MemberVO getById(@Param("id") Long id);
    int updateRoleAndPlace(@Param("userId") Long userId,
                           @Param("role") String role,
                           @Param("placeId") Long placeId);
}
