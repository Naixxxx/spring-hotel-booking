package io.staypilot.reservations.integration;

public class SlotBusyException extends RuntimeException {

    public SlotBusyException(String message) {
        super(message);
    }
}
