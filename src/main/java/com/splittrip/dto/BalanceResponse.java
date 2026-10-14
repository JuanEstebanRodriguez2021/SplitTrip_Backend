package com.splittrip.dto;

import java.math.BigDecimal;

/** Positive balance = is owed money; negative = owes money. */
public record BalanceResponse(ParticipantResponse participant, BigDecimal balance) {
}