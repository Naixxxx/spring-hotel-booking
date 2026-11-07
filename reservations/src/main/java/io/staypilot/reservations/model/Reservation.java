package io.staypilot.reservations.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(name = "uk_booking_request_id", columnNames = "request_id"),
    indexes = {@Index(name = "idx_bookings_user_time", columnList = "user_id,created_at"),
        @Index(name = "idx_bookings_compensation", columnList = "status,compensation_pending")})
public class Reservation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;
    @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 400)
    private String fingerprint;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Account account;
    @Column(name = "room_id")
    private Long roomId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "auto_select", nullable = false)
    private boolean autoSelect;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BookingStatus status;
    @Column(name = "compensation_pending", nullable = false)
    private boolean compensationPending;
    @Column(name = "failure_reason", length = 240)
    private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Version
    private Long version;

    protected Reservation() {
    }

    public Reservation(UUID requestId, String fingerprint, Account account, Long roomId, LocalDate startDate, LocalDate endDate, boolean autoSelect) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.fingerprint = fingerprint;
        this.account = account;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoSelect = autoSelect;
        this.status = BookingStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Account getAccount() {
        return account;
    }

    public Long getRoomId() {
        return roomId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isAutoSelect() {
        return autoSelect;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public boolean isCompensationPending() {
        return compensationPending;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void select(Long id) {
        if (status != BookingStatus.PENDING) throw new IllegalStateException("Booking already finalized");
        roomId = id;
    }

    public void confirm() {
        if (status != BookingStatus.PENDING) throw new IllegalStateException("Invalid booking transition");
        status = BookingStatus.CONFIRMED;
    }

    public void cancel(String reason, boolean releasePending) {
        status = BookingStatus.CANCELLED;
        failureReason = reason;
        compensationPending = releasePending;
    }

    public void releaseCompleted() {
        compensationPending = false;
    }
}
