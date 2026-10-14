package com.splittrip.dto;

import java.math.BigDecimal;

public record TripSummaryResponse(Long id, String name, long participantCount, BigDecimal totalSpent) {
}