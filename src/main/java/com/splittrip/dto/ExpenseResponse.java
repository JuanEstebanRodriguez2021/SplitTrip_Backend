package com.splittrip.dto;

import com.splittrip.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        LocalDate date,
        ParticipantResponse paidBy,
        List<ParticipantResponse> splitAmong) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getDate(),
                ParticipantResponse.from(expense.getPaidBy()),
                expense.getSplitAmong().stream().map(ParticipantResponse::from).toList());
    }
}