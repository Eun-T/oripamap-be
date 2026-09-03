package org.scoula.member.mapper;

import org.scoula.member.dto.ChangePasswordDTO;
import org.apache.ibatis.annotations.Param;
import org.scoula.security.account.domain.MemberVO;

public interface MemberMapper {
    MemberVO get(@Param("email") String email);
    MemberVO findByUsername(@Param("email") String email); // 이메일 중복 체크
    int insert(MemberVO member);  // 회원 정보 추가
    int update(MemberVO member);
    int updatePassword(ChangePasswordDTO changePasswordDTO);

}
