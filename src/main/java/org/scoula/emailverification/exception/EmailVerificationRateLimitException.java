package org.scoula.emailverification.exception;

public class EmailVerificationRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public EmailVerificationRateLimitException(long retryAfterSeconds) {
        super("인증번호는 60초 후에 다시 요청할 수 있습니다.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
