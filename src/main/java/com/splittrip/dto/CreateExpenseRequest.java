package com.splittrip.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** If {@code date} is omitted, today's date is used. */
public record CreateExpenseRequest(
        @NotBlank @Size(max = 120) String description,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        LocalDate date,
        @NotNull Long paidById,
        @NotEmpty List<@NotNull Long> splitAmongIds) {
}