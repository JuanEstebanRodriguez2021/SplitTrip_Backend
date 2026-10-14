package com.splittrip.service;

import com.splittrip.dto.BalanceResponse;
import com.splittrip.dto.ParticipantResponse;
import com.splittrip.dto.SettlementResponse;
import com.splittrip.model.Participant;
import com.splittrip.repository.ExpenseRepository;
import com.splittrip.repository.ParticipantRepository;
import com.splittrip.service.SettlementCalculator.ExpenseInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads a trip's data from the database and delegates the math to SettlementCalculator. */
@Service
@Transactional(readOnly = true)
public class SettlementService {

    private final TripService tripService;
    private final ParticipantRepository participants;
    private final ExpenseRepository expenses;
    private final SettlementCalculator calculator;

    public SettlementService(TripService tripService, ParticipantRepository participants,
                             ExpenseRepository expenses, SettlementCalculator calculator) {
        this.tripService = tripService;
        this.participants = participants;
        this.expenses = expenses;
        this.calculator = calculator;
    }

    public List<BalanceResponse> getBalances(Long tripId) {
        TripLedger ledger = loadLedger(tripId);
        return ledger.members().values().stream()
                .map(p -> new BalanceResponse(ParticipantResponse.from(p), ledger.balances().get(p.getId())))
                .toList();
    }

    public List<SettlementResponse> getSettlements(Long tripId) {
        TripLedger ledger = loadLedger(tripId);
        return calculator.calculateSettlements(ledger.balances()).stream()
                .map(t -> new SettlementResponse(
                        ParticipantResponse.from(ledger.members().get(t.fromId())),
                        ParticipantResponse.from(ledger.members().get(t.toId())),
                        t.amount()))
                .toList();
    }

    private TripLedger loadLedger(Long tripId) {
        tripService.findTrip(tripId);

        Map<Long, Participant> members = new LinkedHashMap<>();
        participants.findByTripIdOrderByName(tripId).forEach(p -> members.put(p.getId(), p));

        List<ExpenseInput> inputs = expenses.findByTripIdOrderByDateDescIdDesc(tripId).stream()
                .map(e -> new ExpenseInput(
                        e.getPaidBy().getId(),
                        e.getAmount(),
                        e.getSplitAmong().stream().map(Participant::getId).toList()))
                .toList();

        return new TripLedger(members, calculator.calculateBalances(members.keySet(), inputs));
    }

    private record TripLedger(Map<Long, Participant> members, Map<Long, BigDecimal> balances) {
    }
}