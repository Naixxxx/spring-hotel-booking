package io.staypilot.catalog.api;

import io.staypilot.catalog.application.SlotLedger;
import io.staypilot.catalog.application.RoomDirectory;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import io.staypilot.contracts.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rooms")
public class InternalSlotEndpoints {

    private final SlotLedger slots;
    private final RoomDirectory rooms;

    public InternalSlotEndpoints(SlotLedger slots, RoomDirectory rooms) {
        this.slots = slots;
        this.rooms = rooms;
    }

    @GetMapping("/recommend")
    public List<RoomCard> ranked(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rooms.available(startDate, endDate, true);
    }

    @PostMapping("/{id}/hold")
    public HoldReceipt hold(@PathVariable Long id, @RequestBody HoldCommand command) {
        return slots.hold(id, command);
    }

    @PostMapping("/{id}/release")
    public HoldReceipt release(@PathVariable Long id, @RequestBody ReleaseCommand command) {
        return slots.release(id, command);
    }
}
