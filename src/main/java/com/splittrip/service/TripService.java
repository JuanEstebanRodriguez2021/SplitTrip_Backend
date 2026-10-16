package com.splittrip.service;

import com.splittrip.dto.AddParticipantRequest;
import com.splittrip.dto.CreateTripRequest;
import com.splittrip.dto.ParticipantResponse;
import com.splittrip.dto.TripResponse;
import com.splittrip.dto.TripSummaryResponse;
import com.splittrip.exception.NotFoundException;
import com.splittrip.model.Participant;
import com.splittrip.model.Trip;
import com.splittrip.model.TripCategory;
import com.splittrip.model.User;
import com.splittrip.repository.ExpenseRepository;
import com.splittrip.repository.ParticipantRepository;
import com.splittrip.repository.TripRepository;
import com.splittrip.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository trips;
    private final ParticipantRepository participants;
    private final ExpenseRepository expenses;
    private final UserRepository users;
    private final TripAccess tripAccess;
    private final SettlementService settlementService;

    public TripService(TripRepository trips, ParticipantRepository participants, ExpenseRepository expenses,
                       UserRepository users, TripAccess tripAccess, SettlementService settlementService) {
        this.trips = trips;
        this.participants = participants;
        this.expenses = expenses;
        this.users = users;
        this.tripAccess = tripAccess;
        this.settlementService = settlementService;
    }

    public List<TripSummaryResponse> listTrips(Long userId) {
        return trips.findByOwnerIdOrderByIdDesc(userId).stream()
                .map(trip -> toSummary(trip, userId))
                .toList();
    }

    public TripResponse getTrip(Long tripId, Long userId) {
        return toResponse(tripAccess.findOwnedTrip(tripId, userId), userId);
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, Long userId) {
        User owner = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String ownerKey = owner.getName().toLowerCase(Locale.ROOT);

        // The creator is added automatically; skip them if they also typed their own name.
        List<String> others = request.participants() == null
                ? List.of()
                : request.participants().stream()
                        .map(String::trim)
                        .filter(name -> !name.toLowerCase(Locale.ROOT).equals(ownerKey))
                        .toList();
        ensureUniqueNames(others);

        TripCategory category = request.category() != null ? request.category() : TripCategory.TRIP;
        Trip trip = trips.save(new Trip(request.name().trim(), category, owner));
        participants.save(new Participant(owner.getName(), trip, owner));
        others.forEach(name -> participants.save(new Participant(name, trip)));
        return toResponse(trip, userId);
    }

    @Transactional
    public ParticipantResponse addParticipant(Long tripId, AddParticipantRequest request, Long userId) {
        Trip trip = tripAccess.findOwnedTrip(tripId, userId);
        String name = request.name().trim();
        if (participants.existsByTripIdAndNameIgnoreCase(tripId, name)) {
            throw new IllegalArgumentException("A participant named '" + name + "' already exists in this trip");
        }
        return ParticipantResponse.from(participants.save(new Participant(name, trip)));
    }

    private TripSummaryResponse toSummary(Trip trip, Long userId) {
        Long tripId = trip.getId();
        List<String> names = participants.findByTripIdOrderByName(tripId).stream()
                .map(Participant::getName)
                .toList();
        return new TripSummaryResponse(
                tripId,
                trip.getName(),
                trip.getCategory(),
                names.size(),
                names,
                expenses.countByTripId(tripId),
                totalSpent(tripId),
                expenses.minDateByTripId(tripId),
                expenses.maxDateByTripId(tripId),
                myBalance(tripId, me(tripId, userId)));
    }

    private TripResponse toResponse(Trip trip, Long userId) {
        Long tripId = trip.getId();
        List<ParticipantResponse> members = participants.findByTripIdOrderByName(tripId).stream()
                .map(ParticipantResponse::from)
                .toList();
        Optional<Participant> me = me(tripId, userId);
        return new TripResponse(
                tripId,
                trip.getName(),
                trip.getCategory(),
                members,
                expenses.countByTripId(tripId),
                totalSpent(tripId),
                expenses.minDateByTripId(tripId),
                expenses.maxDateByTripId(tripId),
                me.map(Participant::getId).orElse(null),
                myBalance(tripId, me));
    }

    private Optional<Participant> me(Long tripId, Long userId) {
        return participants.findByTripIdAndUserId(tripId, userId);
    }

    private BigDecimal myBalance(Long tripId, Optional<Participant> me) {
        return me.map(p -> settlementService.loadLedger(tripId).balances().get(p.getId()))
                .orElse(BigDecimal.ZERO);
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