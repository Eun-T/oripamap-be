package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.MemberVO;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String provider;
    private Date regDate;
    private Date updateDate;
    private List<String> authList;

    //controller, service(dto) <---- mybatis(VO)
    public static MemberDTO of(MemberVO m){
        return MemberDTO.builder()
                .id(m.getId())
                .username(m.getUsername())
                .email(m.getEmail())
                .nickname(m.getNickname())
                .provider(m.getProvider())
                .regDate(m.getRegDate())
                .updateDate(m.getUpdateDate())
                .authList(m.getAuthList().stream().map(a->a.getAuth()).toList())
                .build();
    }

    public MemberVO toVO() {
        return MemberVO.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .regDate(regDate)
                .updateDate(updateDate)
                .build();
    }
}
