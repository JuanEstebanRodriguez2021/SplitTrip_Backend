package com.splittrip.dto;

import com.splittrip.model.TripCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** myParticipantId is the participant linked to the logged-in user. */
public record TripResponse(
        Long id,
        String name,
        TripCategory category,
        List<ParticipantResponse> participants,
        long expenseCount,
        BigDecimal totalSpent,
        LocalDate startDate,
        LocalDate endDate,
        Long myParticipantId,
        BigDecimal myBalance) {
}