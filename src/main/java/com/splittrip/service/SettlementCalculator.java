package com.splittrip.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Core of the app: computes how much each participant owes or is owed,
 * and suggests the transfers needed to settle up.
 *
 * Works only with ids and amounts (not JPA entities) so it is easy to test
 * and does not depend on the database.
 */
@Component
public class SettlementCalculator {

    private static final int SCALE = 2;
    private static final BigDecimal CENT = new BigDecimal("0.01");

    /** Minimal expense data needed for the calculation. */
    public record ExpenseInput(Long payerId, BigDecimal amount, List<Long> splitAmongIds) {
    }

    /** "from pays amount to to". */
    public record Transfer(Long fromId, Long toId, BigDecimal amount) {
    }

    /**
     * Net balance of each participant: what they paid minus their share.
     * Positive = is owed money; negative = owes money. Balances always sum to zero.
     *
     * @param participantIds every participant in the trip (included even with a zero balance)
     * @return map of id -> balance, in the same order as {@code participantIds}
     */
    public Map<Long, BigDecimal> calculateBalances(Collection<Long> participantIds, List<ExpenseInput> expenses) {
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        for (Long id : participantIds) {
            balances.put(id, BigDecimal.ZERO.setScale(SCALE));
        }

        for (ExpenseInput expense : expenses) {
            validate(expense, balances);

            // The payer is credited the full amount...
            balances.merge(expense.payerId(), expense.amount(), BigDecimal::add);

            // ...and everyone sharing the expense is debited their share.
            List<BigDecimal> shares = splitEvenly(expense.amount(), expense.splitAmongIds().size());
            for (int i = 0; i < shares.size(); i++) {
                balances.merge(expense.splitAmongIds().get(i), shares.get(i), BigDecimal::subtract);
            }
        }
        return balances;
    }

    /**
     * Suggests transfers to settle all debts. Greedy strategy: at each step the
     * biggest debtor pays the biggest creditor the smaller of their two amounts.
     * Produces at most (n - 1) transfers.
     */
    public List<Transfer> calculateSettlements(Map<Long, BigDecimal> balances) {
        Comparator<Position> largestFirst = Comparator
                .comparing((Position p) -> p.amount).reversed()
                .thenComparing(p -> p.id);

        PriorityQueue<Position> debtors = new PriorityQueue<>(largestFirst);
        PriorityQueue<Position> creditors = new PriorityQueue<>(largestFirst);
        balances.forEach((id, balance) -> {
            if (balance.signum() < 0) {
                debtors.add(new Position(id, balance.negate()));
            } else if (balance.signum() > 0) {
                creditors.add(new Position(id, balance));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Position debtor = debtors.poll();
            Position creditor = creditors.poll();
            BigDecimal amount = debtor.amount.min(creditor.amount);

            transfers.add(new Transfer(debtor.id, creditor.id, amount));

            BigDecimal debtorLeft = debtor.amount.subtract(amount);
            BigDecimal creditorLeft = creditor.amount.subtract(amount);
            if (debtorLeft.signum() > 0) {
                debtors.add(new Position(debtor.id, debtorLeft));
            }
            if (creditorLeft.signum() > 0) {
                creditors.add(new Position(creditor.id, creditorLeft));
            }
        }
        return transfers;
    }

    /**
     * Splits an amount into n shares that add up exactly to the total.
     * Leftover cents are handed out one by one to the first shares.
     * E.g. 100.00 / 3 -> [33.34, 33.33, 33.33]
     */
    List<BigDecimal> splitEvenly(BigDecimal amount, int n) {
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(base.multiply(BigDecimal.valueOf(n)));
        int leftoverCents = remainder.divide(CENT, 0, RoundingMode.UNNECESSARY).intValueExact();

        List<BigDecimal> shares = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            shares.add(i < leftoverCents ? base.add(CENT) : base);
        }
        return shares;
    }

    private void validate(ExpenseInput expense, Map<Long, BigDecimal> balances) {
        if (expense.amount() == null || expense.amount().signum() <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }
        if (expense.amount().stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than " + SCALE + " decimal places");
        }
        if (expense.splitAmongIds() == null || expense.splitAmongIds().isEmpty()) {
            throw new IllegalArgumentException("Expense must be split among at least one participant");
        }
        if (!balances.containsKey(expense.payerId())) {
            throw new IllegalArgumentException("Payer is not part of this trip: " + expense.payerId());
        }
        for (Long id : expense.splitAmongIds()) {
            if (!balances.containsKey(id)) {
                throw new IllegalArgumentException("Participant is not part of this trip: " + id);
            }
        }
        if (expense.splitAmongIds().stream().distinct().count() != expense.splitAmongIds().size()) {
            throw new IllegalArgumentException("Expense split contains duplicate participants");
        }
    }

    private record Position(Long id, BigDecimal amount) {
    }
}