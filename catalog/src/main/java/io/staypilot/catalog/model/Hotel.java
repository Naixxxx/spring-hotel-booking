package io.staypilot.catalog.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 240)
    private String address;
    @Column(nullable = false, length = 80)
    private String city;

    protected Hotel() {
    }

    public Hotel(String name, String address, String city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public void update(String name, String address, String city) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (city != null) this.city = city;
    }
}
