package com.atinroy.recallr.global;

import com.atinroy.recallr.auth.InvalidTokenException;
import com.atinroy.recallr.common.BadRequestException;
import com.atinroy.recallr.domain.deck.DeckNotFoundException;
import com.atinroy.recallr.domain.notebook.NotebookNotFoundException;
import com.atinroy.recallr.domain.note.NoteLinkNotFoundException;
import com.atinroy.recallr.domain.note.NoteNotFoundException;
import com.atinroy.recallr.domain.user.EmailAlreadyExistsException;
import com.atinroy.recallr.domain.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalControllerAdvice {

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.CONFLICT, e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailure(AuthenticationException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, "Authentication failed"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, "Authentication required"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({NoteNotFoundException.class, NoteLinkNotFoundException.class, NotebookNotFoundException.class, DeckNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException e) {
        return new ResponseEntity<>(errorBody(HttpStatus.BAD_REQUEST, e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
