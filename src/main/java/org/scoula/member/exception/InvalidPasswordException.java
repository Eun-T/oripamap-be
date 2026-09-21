package org.scoula.member.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("비밀번호는 8자 이상이어야 하며 영문, 숫자, 일반 특수문자만 사용할 수 있습니다.");
    }
}
