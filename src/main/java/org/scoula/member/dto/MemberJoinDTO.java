package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.MemberVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberJoinDTO {
    private String username;
    private String password;
    private String email;
    private String nickname;

    //dto(controller, service) --> vo(mybatis)

    public MemberVO toVO(){
        return MemberVO.builder()
                .username(email)
                .password(password)
                .email(email)
                .nickname(nickname == null || nickname.isBlank() ? username : nickname)
                .provider("LOCAL")
                .build();
    }
}
