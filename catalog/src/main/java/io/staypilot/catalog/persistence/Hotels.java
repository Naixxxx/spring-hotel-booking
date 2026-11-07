package io.staypilot.catalog.persistence;

import io.staypilot.catalog.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Hotels extends JpaRepository<Hotel, Long> {

}
