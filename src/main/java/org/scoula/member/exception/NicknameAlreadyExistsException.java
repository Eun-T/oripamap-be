package org.scoula.member.exception;

public class NicknameAlreadyExistsException extends RuntimeException {
    public NicknameAlreadyExistsException() {
        super("이미 사용 중인 닉네임입니다.");
    }

    public NicknameAlreadyExistsException(Throwable cause) {
        super("이미 사용 중인 닉네임입니다.", cause);
    }
}
