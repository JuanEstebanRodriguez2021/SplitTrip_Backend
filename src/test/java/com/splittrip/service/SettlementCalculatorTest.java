package com.splittrip.service;

import com.splittrip.service.SettlementCalculator.ExpenseInput;
import com.splittrip.service.SettlementCalculator.Transfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementCalculatorTest {

    private static final Long ANA = 1L;
    private static final Long LUIS = 2L;
    private static final Long SARA = 3L;
    private static final Long JUAN = 4L;
    private static final List<Long> EVERYONE = List.of(ANA, LUIS, SARA, JUAN);

    private final SettlementCalculator calculator = new SettlementCalculator();

    @Test
    void simpleExpenseSplitAmongThree() {
        var expenses = List.of(expense(ANA, "90000", ANA, LUIS, SARA));

        var balances = calculator.calculateBalances(List.of(ANA, LUIS, SARA), expenses);

        assertThat(balances.get(ANA)).isEqualByComparingTo("60000");
        assertThat(balances.get(LUIS)).isEqualByComparingTo("-30000");
        assertThat(balances.get(SARA)).isEqualByComparingTo("-30000");

        var transfers = calculator.calculateSettlements(balances);
        assertThat(transfers).hasSize(2);
        assertThat(transfers).allSatisfy(t -> {
            assertThat(t.toId()).isEqualTo(ANA);
            assertThat(t.amount()).isEqualByComparingTo("30000");
        });
    }

    @Test
    void noExpensesMeansZeroBalancesAndNoTransfers() {
        var balances = calculator.calculateBalances(EVERYONE, List.of());

        assertThat(balances).hasSize(4);
        assertThat(balances.values()).allSatisfy(b -> assertThat(b).isZero());
        assertThat(calculator.calculateSettlements(balances)).isEmpty();
    }

    @Test
    void payerCanBeLeftOutOfTheSplit() {
        // Ana treats Luis and Sara and doesn't eat anything herself
        var balances = calculator.calculateBalances(
                List.of(ANA, LUIS, SARA), List.of(expense(ANA, "50000", LUIS, SARA)));

        assertThat(balances.get(ANA)).isEqualByComparingTo("50000");
        assertThat(balances.get(LUIS)).isEqualByComparingTo("-25000");
        assertThat(balances.get(SARA)).isEqualByComparingTo("-25000");
    }

    @Test
    void leftoverCentsAddUpExactly() {
        assertThat(calculator.splitEvenly(new BigDecimal("100.00"), 3))
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));

        var balances = calculator.calculateBalances(
                List.of(ANA, LUIS, SARA), List.of(expense(ANA, "100", ANA, LUIS, SARA)));

        assertThat(sum(balances)).isZero();
    }

    @Test
    void chainedDebtsCollapseIntoOneTransfer() {
        // Luis covered Ana, Sara covered Luis: Ana pays Sara directly
        var expenses = List.of(
                expense(LUIS, "100000", ANA),
                expense(SARA, "100000", LUIS));

        var balances = calculator.calculateBalances(List.of(ANA, LUIS, SARA), expenses);
        var transfers = calculator.calculateSettlements(balances);

        assertThat(transfers).containsExactly(
                new Transfer(ANA, SARA, new BigDecimal("100000.00")));
    }

    @Test
    void fullTripSettlesWithAtMostNMinusOneTransfers() {
        var expenses = List.of(
                expense(ANA, "320000", ANA, LUIS, SARA, JUAN),   // hotel
                expense(LUIS, "85500", ANA, LUIS, SARA, JUAN),   // groceries
                expense(SARA, "47000", SARA, JUAN),              // taxi
                expense(JUAN, "123450.50", ANA, LUIS, SARA),     // dinner (Juan skipped it)
                expense(ANA, "10000.01", LUIS));                 // loan

        var balances = calculator.calculateBalances(EVERYONE, expenses);
        assertThat(sum(balances)).isZero();

        var transfers = calculator.calculateSettlements(balances);
        assertThat(transfers).hasSizeLessThanOrEqualTo(EVERYONE.size() - 1);

        // Applying the transfers must leave everyone at exactly zero
        Map<Long, BigDecimal> after = new HashMap<>(balances);
        for (Transfer t : transfers) {
            assertThat(t.amount()).isPositive();
            after.merge(t.fromId(), t.amount(), BigDecimal::add);
            after.merge(t.toId(), t.amount(), BigDecimal::subtract);
        }
        assertThat(after.values()).allSatisfy(b -> assertThat(b).isZero());
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(ANA, "0", LUIS))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(ANA, "-5", LUIS))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(ANA, "10.001", LUIS))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptySplitOrUnknownParticipants() {
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(ANA, "1000"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(ANA, "1000", 99L))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(99L, "1000", ANA))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateBalances(EVERYONE, List.of(expense(ANA, "1000", LUIS, LUIS))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ExpenseInput expense(Long payer, String amount, Long... splitAmong) {
        return new ExpenseInput(payer, new BigDecimal(amount), List.of(splitAmong));
    }

    private static BigDecimal sum(Map<Long, BigDecimal> balances) {
        return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
