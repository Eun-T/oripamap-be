package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.exception.NicknameAlreadyExistsException;
import org.scoula.member.exception.PasswordMissmatchException;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.member.util.NicknamePolicy;
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
        if (!NicknamePolicy.isValid(member.getNickname())) {
            throw new IllegalArgumentException("닉네임은 공백 없이 1자 이상 7자 이하여야 합니다.");
        }
        if (mapper.existsByNickname(member.getNickname())) {
            throw new NicknameAlreadyExistsException();
        }
        member.setPassword(passwordEncoder.encode(member.getPassword())); // 비밀번호 암호화
        try {
            mapper.insert(member);
        } catch (DuplicateKeyException e) {
            if (mapper.existsByEmail(member.getEmail())) {
                throw new EmailAlreadyExistsException(e);
            }
            if (mapper.existsByNickname(member.getNickname())) {
                throw new NicknameAlreadyExistsException(e);
            }
            throw e;
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
    public void updateNickname(Long userId, String nickname) {
        if (!NicknamePolicy.isValid(nickname)) {
            throw new IllegalArgumentException("닉네임은 공백 없이 1자 이상 7자 이하여야 합니다.");
        }
        if (mapper.existsByNicknameExcludingUser(nickname, userId)) {
            throw new NicknameAlreadyExistsException();
        }
        try {
            if (mapper.updateNickname(userId, nickname) != 1) {
                throw new NoSuchElementException();
            }
        } catch (DuplicateKeyException e) {
            throw new NicknameAlreadyExistsException(e);
        }
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
