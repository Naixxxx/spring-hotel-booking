package io.staypilot.contracts;

import org.springframework.http.HttpStatus;

public class DomainError extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public DomainError(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
