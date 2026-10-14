package com.splittrip.dto;

import java.math.BigDecimal;
import java.util.List;

public record TripResponse(Long id, String name, List<ParticipantResponse> participants, BigDecimal totalSpent) {
}