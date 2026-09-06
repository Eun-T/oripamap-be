package org.scoula.member.service;

import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;

public interface MemberService {
    boolean existsByEmail(String email);
    MemberDTO get(String username);
    MemberDTO join(MemberJoinDTO member);
    MemberDTO update(String authenticatedUsername, MemberUpdateDTO member);
    void changePassword(String authenticatedUsername, ChangePasswordDTO changePassword);

}
