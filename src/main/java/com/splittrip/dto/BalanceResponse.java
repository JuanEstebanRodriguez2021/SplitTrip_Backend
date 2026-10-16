package com.splittrip.dto;

import java.math.BigDecimal;

/**
 * totalPaid = what the participant paid; totalShare = what they had to pay.
 * balance = totalPaid - totalShare. Positive = is owed money; negative = owes money.
 */
public record BalanceResponse(
        ParticipantResponse participant,
        BigDecimal totalPaid,
        BigDecimal totalShare,
        BigDecimal balance) {
}