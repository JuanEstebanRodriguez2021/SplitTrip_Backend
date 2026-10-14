package com.splittrip.service;

import com.splittrip.dto.AddParticipantRequest;
import com.splittrip.dto.BalanceResponse;
import com.splittrip.dto.CreateExpenseRequest;
import com.splittrip.dto.CreateTripRequest;
import com.splittrip.dto.ExpenseResponse;
import com.splittrip.dto.TripResponse;
import com.splittrip.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end flow through the services against H2. Each test is rolled back. */
@SpringBootTest
@Transactional
class TripFlowTest {

    @Autowired TripService tripService;
    @Autowired ExpenseService expenseService;
    @Autowired SettlementService settlementService;

    @Test
    void fullFlowFromTripToSettlements() {
        TripResponse trip = tripService.createTrip(
                new CreateTripRequest("Cartagena", List.of("Ana", "Luis", "Sara")));
        Long ana = idOf(trip, "Ana");
        Long luis = idOf(trip, "Luis");
        Long sara = idOf(trip, "Sara");

        expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Hotel", new BigDecimal("90000"), null, ana, List.of(ana, luis, sara)));

        List<BalanceResponse> balances = settlementService.getBalances(trip.id());
        assertThat(balanceOf(balances, "Ana")).isEqualByComparingTo("60000");
        assertThat(balanceOf(balances, "Luis")).isEqualByComparingTo("-30000");
        assertThat(balanceOf(balances, "Sara")).isEqualByComparingTo("-30000");

        assertThat(settlementService.getSettlements(trip.id()))
                .hasSize(2)
                .allSatisfy(s -> {
                    assertThat(s.to().name()).isEqualTo("Ana");
                    assertThat(s.amount()).isEqualByComparingTo("30000");
                });

        assertThat(tripService.getTrip(trip.id()).totalSpent()).isEqualByComparingTo("90000");
    }

    @Test
    void deletingAnExpenseClearsTheBalances() {
        TripResponse trip = tripService.createTrip(new CreateTripRequest("Medellín", List.of("Ana", "Luis")));
        Long ana = idOf(trip, "Ana");
        Long luis = idOf(trip, "Luis");

        ExpenseResponse expense = expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Taxi", new BigDecimal("20000"), null, ana, List.of(ana, luis)));
        expenseService.deleteExpense(trip.id(), expense.id());

        assertThat(expenseService.listExpenses(trip.id())).isEmpty();
        assertThat(settlementService.getSettlements(trip.id())).isEmpty();
    }

    @Test
    void rejectsParticipantsFromAnotherTrip() {
        TripResponse trip = tripService.createTrip(new CreateTripRequest("Trip A", List.of("Ana")));
        TripResponse other = tripService.createTrip(new CreateTripRequest("Trip B", List.of("Luis")));
        Long ana = idOf(trip, "Ana");
        Long outsider = idOf(other, "Luis");

        assertThatThrownBy(() -> expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Lunch", new BigDecimal("10000"), null, outsider, List.of(ana))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Lunch", new BigDecimal("10000"), null, ana, List.of(ana, outsider))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateParticipantNames() {
        assertThatThrownBy(() -> tripService.createTrip(new CreateTripRequest("Dupes", List.of("Ana", "ana"))))
                .isInstanceOf(IllegalArgumentException.class);

        TripResponse trip = tripService.createTrip(new CreateTripRequest("Bogotá", List.of("Ana")));
        assertThatThrownBy(() -> tripService.addParticipant(trip.id(), new AddParticipantRequest(" ANA ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownTripIsNotFound() {
        assertThatThrownBy(() -> tripService.getTrip(999_999L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> settlementService.getBalances(999_999L)).isInstanceOf(NotFoundException.class);
    }

    private static Long idOf(TripResponse trip, String name) {
        return trip.participants().stream()
                .filter(p -> p.name().equals(name))
                .findFirst().orElseThrow()
                .id();
    }

    private static BigDecimal balanceOf(List<BalanceResponse> balances, String name) {
        return balances.stream()
                .filter(b -> b.participant().name().equals(name))
                .findFirst().orElseThrow()
                .balance();
    }
}