package org.scoula.exception;

import org.junit.jupiter.api.Test;
import org.scoula.member.exception.EmailAlreadyExistsException;
import org.scoula.member.exception.NicknameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionAdviceTest {

    @Test
    void returnsConflictForDuplicateEmail() {
        ApiExceptionAdvice advice = new ApiExceptionAdvice();

        ResponseEntity<String> response = advice.handleEmailAlreadyExists(
                new EmailAlreadyExistsException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("\uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC778 \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4.", response.getBody());
    }

    @Test
    void returnsConflictWithMachineReadableCodeForDuplicateNickname() {
        ApiExceptionAdvice advice = new ApiExceptionAdvice();

        ResponseEntity<ApiErrorResponse> response = advice.handleNicknameAlreadyExists(
                new NicknameAlreadyExistsException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("NICKNAME_ALREADY_EXISTS", response.getBody().getCode());
        assertEquals("이미 사용 중인 닉네임입니다.", response.getBody().getMessage());
    }
}
