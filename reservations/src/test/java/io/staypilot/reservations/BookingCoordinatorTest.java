package io.staypilot.reservations;

import io.staypilot.reservations.application.*;
import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.integration.*;
import io.staypilot.reservations.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingCoordinatorTest {

    ReservationSteps steps = mock(ReservationSteps.class);
    AccountService accounts = mock(AccountService.class);
    CatalogPort catalog = mock(CatalogPort.class);
    BookingCoordinator coordinator = new BookingCoordinator(steps, accounts, catalog);
    Account user = new Account("traveler", "test", Role.USER);
    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(10);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(user, "id", 7L);
        when(accounts.get("traveler")).thenReturn(user);
    }

    @Test
    void successfulFlowHasTwoLocalStateChangesAndOneRemoteHold() {
        NewBooking request = new NewBooking(UUID.randomUUID(), 15L, false, from, from.plusDays(2));
        when(steps.start(eq(7L), eq(request), anyString())).thenReturn(new ReservationSteps.Begin(id, true));
        when(steps.get(id)).thenReturn(view(15L, BookingStatus.CONFIRMED));
        assertThat(coordinator.reserve("traveler", request).status()).isEqualTo(BookingStatus.CONFIRMED);
        var order = inOrder(steps, catalog);
        order.verify(steps).select(id, 15L);
        order.verify(catalog).hold(15L, id, from, from.plusDays(2));
        order.verify(steps).confirm(id);
        verify(catalog, never()).release(anyLong(), any(), anyBoolean());
    }

    @Test
    void timeoutCancelsAndReleasesUncertainSlot() {
        NewBooking request = new NewBooking(UUID.randomUUID(), 19L, false, from, from.plusDays(1));
        when(steps.start(eq(7L), eq(request), anyString())).thenReturn(new ReservationSteps.Begin(id, true));
        doThrow(new RemoteHotelTransportException("timeout", new RuntimeException())).when(catalog)
            .hold(19L, id, from, from.plusDays(1));
        assertThatThrownBy(() -> coordinator.reserve("traveler", request)).hasMessageContaining("Room verification failed");
        verify(steps).cancelled(id, "HOLD_UNCERTAIN", true);
        verify(catalog).release(19L, id, true);
        verify(steps).released(id);
        verify(steps, never()).confirm(id);
    }

    @Test
    void duplicateRequestDoesNotContactInventoryAgain() {
        NewBooking request = new NewBooking(UUID.randomUUID(), 11L, false, from, from.plusDays(1));
        when(steps.start(eq(7L), eq(request), anyString())).thenReturn(new ReservationSteps.Begin(id, false));
        when(steps.get(id)).thenReturn(view(11L, BookingStatus.PENDING));
        assertThat(coordinator.reserve("traveler", request).status()).isEqualTo(BookingStatus.PENDING);
        verifyNoInteractions(catalog);
    }

    @Test
    void autoSelectionSkipsConflictedRoom() {
        NewBooking request = new NewBooking(UUID.randomUUID(), null, true, from, from.plusDays(1));
        when(steps.start(eq(7L), eq(request), anyString())).thenReturn(new ReservationSteps.Begin(id, true));
        when(catalog.rankedRooms(from, from.plusDays(1))).thenReturn(List.of(
            new io.staypilot.contracts.RoomCard(1L, 1L, "Test", "101", true, 0, 2),
            new io.staypilot.contracts.RoomCard(2L, 1L, "Test", "102", true, 1, 2)));
        doThrow(new SlotBusyException("taken")).when(catalog).hold(1L, id, from, from.plusDays(1));
        when(steps.get(id)).thenReturn(view(2L, BookingStatus.CONFIRMED));
        assertThat(coordinator.reserve("traveler", request).roomId()).isEqualTo(2L);
        verify(catalog).hold(2L, id, from, from.plusDays(1));
        verify(steps).confirm(id);
    }

    @Test
    void cancellationReleasesSlotButKeepsHistoricalBookingCount() {
        when(steps.get(id)).thenReturn(view(17L, BookingStatus.CONFIRMED), view(17L, BookingStatus.CANCELLED));
        coordinator.cancel("traveler", id);
        verify(steps).cancelled(id, "USER_CANCELLED", true);
        verify(catalog).release(17L, id, false);
        verify(steps).released(id);
    }

    private BookingView view(Long roomId, BookingStatus status) {
        return new BookingView(id, UUID.randomUUID(), 7L, roomId, from, from.plusDays(1), status, false, null, java.time.Instant.now());
    }
}
