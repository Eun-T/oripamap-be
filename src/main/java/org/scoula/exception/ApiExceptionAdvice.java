package org.scoula.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Order(2)
@Log4j2
public class ApiExceptionAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        log.warn("API request failed: {}", e.getReason(), e);
        return ResponseEntity
                .status(e.getStatus())
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(e.getReason());
    }

    //404, 500 어노테이션별로 메서드 정의해서 설정
    // 존재하지 않는 URL 요청인 경우
    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("요청한 API 주소가 존재하지 않습니다.");

    }
    // 그 외 서버 내부 오류
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<String> handleException(Exception e) {
        log.error("Unhandled API exception", e);
        return ResponseEntity

                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("서버 내부 오류가 발생했습니다.");

    }
}
