package io.staypilot.contracts;

import java.time.LocalDate;
import java.util.UUID;

public record HoldCommand(UUID bookingId, LocalDate startDate, LocalDate endDate) {
}
