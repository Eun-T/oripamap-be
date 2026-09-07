package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.exception.PasswordMissmatchException;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.refresh.service.RefreshTokenService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MemberServiceImpl  implements MemberService {

    final PasswordEncoder passwordEncoder;
    final MemberMapper mapper;
    final RefreshTokenService refreshTokenService;

    @Override
    public boolean existsByEmail(String email) {
        return mapper.existsByEmail(email);
    }

    @Override
    public MemberDTO get(String username) {
        MemberVO member = Optional.ofNullable(mapper.get(username))
                .orElseThrow(NoSuchElementException::new);
        return MemberDTO.of(member);
    }

    @Transactional
    @Override
    public MemberDTO join(MemberJoinDTO dto) {
        MemberVO member = dto.toVO();
        member.setPassword(passwordEncoder.encode(member.getPassword())); // 비밀번호 암호화
        try {
            mapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw new EmailAlreadyExistsException(e);
        }
        return get(member.getEmail());
    }

    @Override
    public MemberDTO update(String authenticatedUsername, MemberUpdateDTO member) {
        MemberVO vo = mapper.get(authenticatedUsername);
        if(!passwordEncoder.matches(member.getPassword(),vo.getPassword())) {  // 비밀번호 일치 확인
            throw new PasswordMissmatchException();
        }
        member.setUsername(authenticatedUsername);
        try {
            mapper.update(member.toVO());
        } catch (DuplicateKeyException e) {
            throw new EmailAlreadyExistsException(e);
        }
        return get(member.getEmail());
    }

    @Transactional
    @Override
    public void changePassword(String authenticatedUsername, ChangePasswordDTO changePassword) {
        MemberVO member = mapper.get(authenticatedUsername);

        if(!passwordEncoder.matches(changePassword.getOldPassword(), member.getPassword())) {
            throw new PasswordMissmatchException();
        }

        changePassword.setUsername(authenticatedUsername);
        changePassword.setNewPassword(passwordEncoder.encode(changePassword.getNewPassword()));

        mapper.updatePassword(changePassword);
        refreshTokenService.revokeAllByUserId(member.getId());
    }


}
