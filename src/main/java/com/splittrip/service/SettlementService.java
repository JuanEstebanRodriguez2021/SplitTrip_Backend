package com.splittrip.service;

import com.splittrip.dto.BalanceResponse;
import com.splittrip.dto.ParticipantResponse;
import com.splittrip.dto.SettlementResponse;
import com.splittrip.model.Expense;
import com.splittrip.model.Participant;
import com.splittrip.repository.ExpenseRepository;
import com.splittrip.repository.ParticipantRepository;
import com.splittrip.service.SettlementCalculator.ExpenseInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads a trip's data from the database and delegates the math to SettlementCalculator. */
@Service
@Transactional(readOnly = true)
public class SettlementService {

    private final TripAccess tripAccess;
    private final ParticipantRepository participants;
    private final ExpenseRepository expenses;
    private final SettlementCalculator calculator;

    public SettlementService(TripAccess tripAccess, ParticipantRepository participants,
                             ExpenseRepository expenses, SettlementCalculator calculator) {
        this.tripAccess = tripAccess;
        this.participants = participants;
        this.expenses = expenses;
        this.calculator = calculator;
    }

    public List<BalanceResponse> getBalances(Long tripId, Long userId) {
        tripAccess.findOwnedTrip(tripId, userId);
        Ledger ledger = loadLedger(tripId);
        return ledger.members().values().stream()
                .map(p -> {
                    BigDecimal paid = ledger.paid().get(p.getId());
                    BigDecimal balance = ledger.balances().get(p.getId());
                    return new BalanceResponse(ParticipantResponse.from(p), paid, paid.subtract(balance), balance);
                })
                .toList();
    }

    public List<SettlementResponse> getSettlements(Long tripId, Long userId) {
        tripAccess.findOwnedTrip(tripId, userId);
        Ledger ledger = loadLedger(tripId);
        return calculator.calculateSettlements(ledger.balances()).stream()
                .map(t -> new SettlementResponse(
                        ParticipantResponse.from(ledger.members().get(t.fromId())),
                        ParticipantResponse.from(ledger.members().get(t.toId())),
                        t.amount()))
                .toList();
    }

    /** Members, amount paid and balance of each participant. Callers must check access first. */
    Ledger loadLedger(Long tripId) {
        Map<Long, Participant> members = new LinkedHashMap<>();
        Map<Long, BigDecimal> paid = new HashMap<>();
        for (Participant p : participants.findByTripIdOrderByName(tripId)) {
            members.put(p.getId(), p);
            paid.put(p.getId(), BigDecimal.ZERO.setScale(2));
        }

        List<ExpenseInput> inputs = new ArrayList<>();
        for (Expense e : expenses.findByTripIdOrderByDateDescIdDesc(tripId)) {
            paid.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);
            inputs.add(new ExpenseInput(
                    e.getPaidBy().getId(),
                    e.getAmount(),
                    e.getSplitAmong().stream().map(Participant::getId).toList()));
        }

        return new Ledger(members, paid, calculator.calculateBalances(members.keySet(), inputs));
    }

    record Ledger(Map<Long, Participant> members, Map<Long, BigDecimal> paid, Map<Long, BigDecimal> balances) {
    }
}