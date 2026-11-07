package io.staypilot.contracts;

import java.util.UUID;

public record HoldReceipt(UUID bookingId, Long roomId, String status) {
}
