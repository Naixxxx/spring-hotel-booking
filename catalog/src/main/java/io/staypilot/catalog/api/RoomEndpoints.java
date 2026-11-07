package io.staypilot.catalog.api;

import io.staypilot.catalog.application.RoomDirectory;
import io.staypilot.catalog.api.CatalogDtos.*;
import io.staypilot.contracts.RoomCard;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomEndpoints {

    private final RoomDirectory rooms;

    public RoomEndpoints(RoomDirectory rooms) {
        this.rooms = rooms;
    }

    @GetMapping
    public List<RoomCard> available(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rooms.available(startDate, endDate, false);
    }

    @GetMapping("/recommend")
    public List<RoomCard> ranked(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rooms.available(startDate, endDate, true);
    }

    @GetMapping("/{id}")
    public RoomCard one(@PathVariable Long id) {
        return rooms.one(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomCard create(@Valid @RequestBody RoomWrite input) {
        return rooms.add(input);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomCard edit(@PathVariable Long id, @Valid @RequestBody RoomPatch input) {
        return rooms.edit(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        rooms.remove(id);
    }
}
