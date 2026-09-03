package org.scoula.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.MemberVO;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserInfoDTO {
    Long id;
    String username;
    String email;
    String nickname;
    List<String> roles;

    public static UserInfoDTO of(MemberVO member){

        return new UserInfoDTO(
                member.getId(),
                member.getUsername(),
                member.getEmail(),
                member.getNickname(),
                member.getAuthList().stream().map(a -> a.getAuth()).toList() // {"role_admin", "role_member"}
                );
    }

}
