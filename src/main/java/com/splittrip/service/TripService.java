package com.splittrip.service;

import com.splittrip.dto.AddParticipantRequest;
import com.splittrip.dto.CreateTripRequest;
import com.splittrip.dto.ParticipantResponse;
import com.splittrip.dto.TripResponse;
import com.splittrip.dto.TripSummaryResponse;
import com.splittrip.exception.NotFoundException;
import com.splittrip.model.Participant;
import com.splittrip.model.Trip;
import com.splittrip.repository.ExpenseRepository;
import com.splittrip.repository.ParticipantRepository;
import com.splittrip.repository.TripRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository trips;
    private final ParticipantRepository participants;
    private final ExpenseRepository expenses;

    public TripService(TripRepository trips, ParticipantRepository participants, ExpenseRepository expenses) {
        this.trips = trips;
        this.participants = participants;
        this.expenses = expenses;
    }

    public List<TripSummaryResponse> listTrips() {
        return trips.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(trip -> new TripSummaryResponse(
                        trip.getId(),
                        trip.getName(),
                        participants.countByTripId(trip.getId()),
                        totalSpent(trip.getId())))
                .toList();
    }

    public TripResponse getTrip(Long tripId) {
        return toResponse(findTrip(tripId));
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        List<String> names = request.participants() == null
                ? List.of()
                : request.participants().stream().map(String::trim).toList();
        ensureUniqueNames(names);

        Trip trip = trips.save(new Trip(request.name().trim()));
        names.forEach(name -> participants.save(new Participant(name, trip)));
        return toResponse(trip);
    }

    @Transactional
    public ParticipantResponse addParticipant(Long tripId, AddParticipantRequest request) {
        Trip trip = findTrip(tripId);
        String name = request.name().trim();
        if (participants.existsByTripIdAndNameIgnoreCase(tripId, name)) {
            throw new IllegalArgumentException("A participant named '" + name + "' already exists in this trip");
        }
        return ParticipantResponse.from(participants.save(new Participant(name, trip)));
    }

    /** Shared by the other services to check that a trip exists. */
    public Trip findTrip(Long tripId) {
        return trips.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));
    }

    private TripResponse toResponse(Trip trip) {
        List<ParticipantResponse> members = participants.findByTripIdOrderByName(trip.getId()).stream()
                .map(ParticipantResponse::from)
                .toList();
        return new TripResponse(trip.getId(), trip.getName(), members, totalSpent(trip.getId()));
    }

    private BigDecimal totalSpent(Long tripId) {
        BigDecimal total = expenses.sumAmountByTripId(tripId);
        return total != null ? total : BigDecimal.ZERO;
    }

    private static void ensureUniqueNames(List<String> names) {
        Set<String> seen = new HashSet<>();
        for (String name : names) {
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate participant name: " + name);
            }
        }
    }
}