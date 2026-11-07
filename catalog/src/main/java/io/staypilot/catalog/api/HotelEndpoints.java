package io.staypilot.catalog.api;

import io.staypilot.catalog.application.HotelDirectory;
import io.staypilot.catalog.api.CatalogDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelEndpoints {

    private final HotelDirectory hotels;

    public HotelEndpoints(HotelDirectory hotels) {
        this.hotels = hotels;
    }

    @GetMapping
    public List<HotelView> all() {
        return hotels.all();
    }

    @GetMapping("/{id}")
    public HotelView one(@PathVariable Long id) {
        return hotels.one(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public HotelView create(@Valid @RequestBody HotelWrite input) {
        return hotels.add(input);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelView update(@PathVariable Long id, @RequestBody HotelPatch input) {
        return hotels.edit(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        hotels.remove(id);
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelStatistics statistics(@PathVariable Long id) {
        return hotels.statistics(id);
    }
}
