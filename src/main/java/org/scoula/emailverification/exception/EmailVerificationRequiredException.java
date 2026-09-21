package org.scoula.emailverification.exception;

public class EmailVerificationRequiredException extends RuntimeException {
    public EmailVerificationRequiredException() {
        super("이메일 인증이 필요하거나 인증 유효시간이 만료되었습니다.");
    }
}
