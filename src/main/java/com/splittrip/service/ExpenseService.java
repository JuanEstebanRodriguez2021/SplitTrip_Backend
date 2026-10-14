package com.splittrip.service;

import com.splittrip.dto.CreateExpenseRequest;
import com.splittrip.dto.ExpenseResponse;
import com.splittrip.exception.NotFoundException;
import com.splittrip.model.Expense;
import com.splittrip.model.Participant;
import com.splittrip.model.Trip;
import com.splittrip.repository.ExpenseRepository;
import com.splittrip.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final TripService tripService;
    private final ParticipantRepository participants;
    private final ExpenseRepository expenses;

    public ExpenseService(TripService tripService, ParticipantRepository participants, ExpenseRepository expenses) {
        this.tripService = tripService;
        this.participants = participants;
        this.expenses = expenses;
    }

    public List<ExpenseResponse> listExpenses(Long tripId) {
        tripService.findTrip(tripId);
        return expenses.findByTripIdOrderByDateDescIdDesc(tripId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional
    public ExpenseResponse createExpense(Long tripId, CreateExpenseRequest request) {
        Trip trip = tripService.findTrip(tripId);
        Map<Long, Participant> members = participants.findByTripIdOrderByName(tripId).stream()
                .collect(Collectors.toMap(Participant::getId, Function.identity()));

        Participant paidBy = members.get(request.paidById());
        if (paidBy == null) {
            throw new IllegalArgumentException("Payer is not part of this trip: " + request.paidById());
        }

        List<Long> splitIds = request.splitAmongIds();
        if (new HashSet<>(splitIds).size() != splitIds.size()) {
            throw new IllegalArgumentException("Expense split contains duplicate participants");
        }

        List<Participant> splitAmong = new ArrayList<>();
        for (Long id : splitIds) {
            Participant participant = members.get(id);
            if (participant == null) {
                throw new IllegalArgumentException("Participant is not part of this trip: " + id);
            }
            splitAmong.add(participant);
        }

        // @Digits guarantees at most 2 decimals, so setScale never rounds here.
        BigDecimal amount = request.amount().setScale(2);
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();

        Expense expense = expenses.save(new Expense(
                request.description().trim(), amount, date, trip, paidBy, splitAmong));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void deleteExpense(Long tripId, Long expenseId) {
        Expense expense = expenses.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new NotFoundException("Expense not found: " + expenseId));
        expenses.delete(expense);
    }
}