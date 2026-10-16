package com.splittrip.dto;

import com.splittrip.model.TripCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Card on the home screen. startDate/endDate come from the first and last expense (null if none).
 * myBalance: positive = they owe you; negative = you owe.
 */
public record TripSummaryResponse(
        Long id,
        String name,
        TripCategory category,
        int participantCount,
        List<String> participantNames,
        long expenseCount,
        BigDecimal totalSpent,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal myBalance) {
}