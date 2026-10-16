package com.splittrip.service;

import com.splittrip.exception.NotFoundException;
import com.splittrip.model.Trip;
import com.splittrip.repository.TripRepository;
import org.springframework.stereotype.Component;

/**
 * Loads a trip only if it belongs to the given user. Other users get 404,
 * so they can't even tell the trip exists.
 */
@Component
public class TripAccess {

    private final TripRepository trips;

    public TripAccess(TripRepository trips) {
        this.trips = trips;
    }

    public Trip findOwnedTrip(Long tripId, Long userId) {
        return trips.findByIdAndOwnerId(tripId, userId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));
    }
}