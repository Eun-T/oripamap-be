package org.scoula.exception;

import org.scoula.member.exception.EmailAlreadyExistsException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Order(2)
@Log4j2
public class ApiExceptionAdvice {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    protected ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("\uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC778 \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<String> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("JSON 요청 본문이 올바르지 않습니다. 이미지 파일은 multipart/form-data로 전송해 주세요.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<String> handleUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("업로드 요청의 허용 크기를 초과했습니다.");
    }

    @ExceptionHandler(MultipartException.class)
    protected ResponseEntity<String> handleMultipart(MultipartException e) {
        return ResponseEntity.badRequest()
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("파일 업로드 요청 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    protected ResponseEntity<String> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("지원하지 않는 Content-Type입니다.");
    }

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
