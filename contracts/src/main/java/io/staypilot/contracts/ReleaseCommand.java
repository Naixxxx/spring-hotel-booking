package io.staypilot.contracts;

import java.util.UUID;

public record ReleaseCommand(UUID bookingId, boolean compensation) {
}
