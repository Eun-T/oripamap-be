package org.scoula.member.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Email is already in use.");
    }

    public EmailAlreadyExistsException(Throwable cause) {
        super("Email is already in use.", cause);
    }
}
