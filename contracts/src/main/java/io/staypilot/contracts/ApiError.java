package io.staypilot.contracts;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String code, String message,
                       String path, String correlationId) {
}
