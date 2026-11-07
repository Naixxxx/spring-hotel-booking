package io.staypilot.catalog.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_hotel_number", columnNames = {"hotel_id", "number"}),
    indexes = {@Index(name = "idx_room_ranking", columnList = "available,times_booked,id")})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;
    @Column(nullable = false, length = 20)
    private String number;
    @Column(nullable = false)
    private boolean available;
    @Column(name = "times_booked", nullable = false)
    private int timesBooked;
    @Column(nullable = false)
    private int capacity;
    @Version
    private long version;

    protected Room() {
    }

    public Room(Hotel hotel, String number, int capacity) {
        this.hotel = hotel;
        this.number = number;
        this.capacity = capacity;
        this.available = true;
    }

    public Long getId() {
        return id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public String getNumber() {
        return number;
    }

    public boolean isAvailable() {
        return available;
    }

    public int getTimesBooked() {
        return timesBooked;
    }

    public int getCapacity() {
        return capacity;
    }

    public void update(String number, Boolean available, Integer capacity) {
        if (number != null) this.number = number;
        if (available != null) this.available = available;
        if (capacity != null) this.capacity = capacity;
    }

    public void acquired() {
        timesBooked++;
    }

    public void compensated() {
        if (timesBooked > 0) timesBooked--;
    }

    public void seedHistoricalCount(int count) {
        timesBooked = count;
    }
}
