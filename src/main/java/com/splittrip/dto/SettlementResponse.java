package com.splittrip.dto;

import java.math.BigDecimal;

/** "from pays amount to to". */
public record SettlementResponse(ParticipantResponse from, ParticipantResponse to, BigDecimal amount) {
}