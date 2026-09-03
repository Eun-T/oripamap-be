package org.scoula.security.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.account.mapper.UserDetailsMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDetailsMapper mapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        MemberVO vo = mapper.get(email);
        if(vo == null) {
            throw new UsernameNotFoundException(email + "은 등록되지 않은 이메일입니다.");
        }
        return new CustomUser(vo);
    }

    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        MemberVO vo = mapper.getById(id);
        if (vo == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + id);
        }
        return new CustomUser(vo);
    }
}

