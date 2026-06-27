package com.atinroy.recallr.global;

import com.atinroy.recallr.auth.InvalidTokenException;
import com.atinroy.recallr.user.EmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.core.AuthenticationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalControllerAdvice {

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.CONFLICT, e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleTokenDoesNotExist(InvalidTokenException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication failed");
        detail.setTitle("Unauthorized");
        return detail;
    }
}
