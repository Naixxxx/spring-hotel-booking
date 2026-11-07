package io.staypilot.catalog;

import io.staypilot.catalog.application.*;
import io.staypilot.catalog.model.Room;
import io.staypilot.catalog.persistence.Rooms;
import io.staypilot.contracts.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class SlotLedgerIntegrationTest {

    @Autowired
    SlotLedger slots;
    @Autowired
    Rooms rooms;
    @Autowired
    RoomDirectory directory;

    @Test
    void simultaneousRequestsForSameDatesCannotDoubleBook() throws Exception {
        Room room = rooms.findByAvailableTrueOrderByTimesBookedAscIdAsc().get(0);
        LocalDate start = LocalDate.now().plusDays(83), end = start.plusDays(3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), startSignal = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(), conflict = new AtomicInteger();
        try {
            Future<?> a = executor.submit(() -> attempt(room.getId(), start, end, ready, startSignal, success, conflict));
            Future<?> b = executor.submit(() -> attempt(room.getId(), start, end, ready, startSignal, success, conflict));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            startSignal.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
            assertThat(success.get()).isEqualTo(1);
            assertThat(conflict.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private void attempt(Long roomId, LocalDate from, LocalDate to, CountDownLatch ready, CountDownLatch signal,
                         AtomicInteger success, AtomicInteger conflict) {
        ready.countDown();
        try {
            if (!signal.await(5, TimeUnit.SECONDS)) throw new AssertionError("Barrier timeout");
            slots.hold(roomId, new HoldCommand(UUID.randomUUID(), from, to));
            success.incrementAndGet();
        } catch (DomainError error) {
            if (error.status() != HttpStatus.CONFLICT) throw error;
            conflict.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Test
    void replayAndCompensationAreIdempotent() {
        Room room = rooms.findByAvailableTrueOrderByTimesBookedAscIdAsc().get(1);
        UUID id = UUID.randomUUID();
        LocalDate from = LocalDate.now().plusDays(121);
        HoldCommand cmd = new HoldCommand(id, from, from.plusDays(2));
        int initial = room.getTimesBooked();
        slots.hold(room.getId(), cmd);
        slots.hold(room.getId(), cmd);
        assertThat(rooms.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(initial + 1);
        slots.release(room.getId(), new ReleaseCommand(id, true));
        slots.release(room.getId(), new ReleaseCommand(id, true));
        assertThat(rooms.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(initial);
        assertThatThrownBy(() -> slots.hold(room.getId(), cmd)).isInstanceOf(DomainError.class);
    }

    @Test
    void recommendationsAreSortedByHistoricalBookingsThenId() {
        LocalDate date = LocalDate.now().plusDays(230);
        var list = directory.available(date, date.plusDays(1), true);
        for (int index = 1; index < list.size(); index++) {
            RoomCard prior = list.get(index - 1), next = list.get(index);
            assertThat(prior.timesBooked() < next.timesBooked() ||
                (prior.timesBooked() == next.timesBooked() && prior.id() < next.id())).isTrue();
        }
    }

    @Test
    void releaseBeforeDelayedHoldActsAsTombstone() {
        Room room = rooms.findByAvailableTrueOrderByTimesBookedAscIdAsc().get(2);
        UUID id = UUID.randomUUID();
        LocalDate from = LocalDate.now().plusDays(150);
        slots.release(room.getId(), new ReleaseCommand(id, true));
        assertThatThrownBy(() -> slots.hold(room.getId(), new HoldCommand(id, from, from.plusDays(2))))
            .isInstanceOf(DomainError.class).hasMessageContaining("processed or released");
    }
}
