package com.splittrip.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** A person within a trip. */
@Entity
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    protected Participant() {
    }

    public Participant(String name, Trip trip) {
        this.name = name;
        this.trip = trip;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Trip getTrip() {
        return trip;
    }
}