package org.scoula.member.mapper;

import org.scoula.member.dto.ChangePasswordDTO;
import org.apache.ibatis.annotations.Param;
import org.scoula.security.account.domain.MemberVO;

public interface MemberMapper {
    MemberVO get(@Param("email") String email);
    boolean existsByEmail(@Param("email") String email);
    boolean existsByNickname(@Param("nickname") String nickname);
    boolean existsByNicknameExcludingUser(@Param("nickname") String nickname,
                                          @Param("userId") Long userId);
    int insert(MemberVO member);  // 회원 정보 추가
    int update(MemberVO member);
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
    int updatePassword(ChangePasswordDTO changePasswordDTO);
    MemberVO findByProvider(@Param("provider") String provider,
                            @Param("providerId") String providerId);
    int insertSocial(MemberVO member);

}
