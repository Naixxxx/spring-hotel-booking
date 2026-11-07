package io.staypilot.contracts;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainError.class)
    public ResponseEntity<ApiError> domain(DomainError error, HttpServletRequest request) {
        return make(error.status(), error.code(), error.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException error, HttpServletRequest request) {
        String message = error.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).findFirst().orElse("Invalid request");
        return make(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException error, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(error.getStatusCode().value());
        return make(status, "REQUEST_FAILED", error.getReason(), request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(org.springframework.dao.DataIntegrityViolationException error, HttpServletRequest request) {
        return make(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", "Operation violates a database constraint", request);
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> locked(org.springframework.dao.PessimisticLockingFailureException error, HttpServletRequest request) {
        return make(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The requested resource is currently being modified", request);
    }

    @ExceptionHandler({IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<ApiError> invalidArgument(Exception error, HttpServletRequest request) {
        return make(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", error.getMessage(), request);
    }

    private ResponseEntity<ApiError> make(HttpStatus status, String code, String detail, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code,
            detail, request.getRequestURI(), MDC.get("correlationId")));
    }
}
