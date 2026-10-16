package com.splittrip.service;

import com.splittrip.dto.AddParticipantRequest;
import com.splittrip.dto.BalanceResponse;
import com.splittrip.dto.CreateExpenseRequest;
import com.splittrip.dto.CreateTripRequest;
import com.splittrip.dto.ExpenseResponse;
import com.splittrip.dto.TripResponse;
import com.splittrip.dto.TripSummaryResponse;
import com.splittrip.exception.NotFoundException;
import com.splittrip.model.TripCategory;
import com.splittrip.model.User;
import com.splittrip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end flow through the services against H2. Each test is rolled back. */
@SpringBootTest
@Transactional
class TripFlowTest {

    @Autowired UserRepository users;
    @Autowired TripService tripService;
    @Autowired ExpenseService expenseService;
    @Autowired SettlementService settlementService;

    private Long camila;

    @BeforeEach
    void createUser() {
        camila = verifiedUser("Camila", "camila@example.com");
    }

    @Test
    void fullFlowFromTripToSettlements() {
        TripResponse trip = tripService.createTrip(
                new CreateTripRequest("Cartagena", TripCategory.TRIP, List.of("Luis", "Sara")), camila);
        Long me = idOf(trip, "Camila");
        Long luis = idOf(trip, "Luis");
        Long sara = idOf(trip, "Sara");
        assertThat(trip.myParticipantId()).isEqualTo(me);

        expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Hotel", new BigDecimal("90000"), null, me, List.of(me, luis, sara)), camila);

        List<BalanceResponse> balances = settlementService.getBalances(trip.id(), camila);
        BalanceResponse mine = balanceOf(balances, "Camila");
        assertThat(mine.totalPaid()).isEqualByComparingTo("90000");
        assertThat(mine.totalShare()).isEqualByComparingTo("30000");
        assertThat(mine.balance()).isEqualByComparingTo("60000");
        assertThat(balanceOf(balances, "Luis").balance()).isEqualByComparingTo("-30000");

        assertThat(settlementService.getSettlements(trip.id(), camila))
                .hasSize(2)
                .allSatisfy(s -> {
                    assertThat(s.to().name()).isEqualTo("Camila");
                    assertThat(s.amount()).isEqualByComparingTo("30000");
                });

        TripResponse detail = tripService.getTrip(trip.id(), camila);
        assertThat(detail.totalSpent()).isEqualByComparingTo("90000");
        assertThat(detail.myBalance()).isEqualByComparingTo("60000");
        assertThat(detail.expenseCount()).isEqualTo(1);
        assertThat(detail.startDate()).isEqualTo(LocalDate.now());

        TripSummaryResponse card = tripService.listTrips(camila).get(0);
        assertThat(card.category()).isEqualTo(TripCategory.TRIP);
        assertThat(card.participantNames()).containsExactly("Camila", "Luis", "Sara");
        assertThat(card.myBalance()).isEqualByComparingTo("60000");
    }

    @Test
    void creatorIsAddedOnceEvenIfTheyTypeTheirOwnName() {
        TripResponse trip = tripService.createTrip(
                new CreateTripRequest("Guatapé", null, List.of("camila", "Luis")), camila);

        assertThat(trip.participants()).extracting(p -> p.name()).containsExactly("Camila", "Luis");
        assertThat(trip.category()).isEqualTo(TripCategory.TRIP);
    }

    @Test
    void myBalanceIsNegativeWhenIOwe() {
        TripResponse trip = tripService.createTrip(
                new CreateTripRequest("Cena", TripCategory.DINNER, List.of("Luis")), camila);
        Long me = idOf(trip, "Camila");
        Long luis = idOf(trip, "Luis");

        expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Pizza", new BigDecimal("30000"), null, luis, List.of(me, luis)), camila);

        assertThat(tripService.getTrip(trip.id(), camila).myBalance()).isEqualByComparingTo("-15000");
    }

    @Test
    void deletingAnExpenseClearsTheBalances() {
        TripResponse trip = tripService.createTrip(
                new CreateTripRequest("Medellín", TripCategory.TRIP, List.of("Luis")), camila);
        Long me = idOf(trip, "Camila");
        Long luis = idOf(trip, "Luis");

        ExpenseResponse expense = expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Taxi", new BigDecimal("20000"), null, me, List.of(me, luis)), camila);
        expenseService.deleteExpense(trip.id(), expense.id(), camila);

        assertThat(expenseService.listExpenses(trip.id(), camila)).isEmpty();
        assertThat(settlementService.getSettlements(trip.id(), camila)).isEmpty();
    }

    @Test
    void rejectsParticipantsFromAnotherTrip() {
        TripResponse trip = tripService.createTrip(new CreateTripRequest("Trip A", null, List.of()), camila);
        TripResponse other = tripService.createTrip(new CreateTripRequest("Trip B", null, List.of("Luis")), camila);
        Long me = idOf(trip, "Camila");
        Long outsider = idOf(other, "Luis");

        assertThatThrownBy(() -> expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Lunch", new BigDecimal("10000"), null, outsider, List.of(me)), camila))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Lunch", new BigDecimal("10000"), null, me, List.of(me, outsider)), camila))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateParticipantNames() {
        assertThatThrownBy(() -> tripService.createTrip(
                new CreateTripRequest("Dupes", null, List.of("Ana", "ana")), camila))
                .isInstanceOf(IllegalArgumentException.class);

        TripResponse trip = tripService.createTrip(new CreateTripRequest("Bogotá", null, List.of("Ana")), camila);
        assertThatThrownBy(() -> tripService.addParticipant(trip.id(), new AddParticipantRequest(" ANA "), camila))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void otherUsersCannotSeeTheTrip() {
        TripResponse trip = tripService.createTrip(
                new CreateTripRequest("Privado", null, List.of("Luis")), camila);
        Long me = idOf(trip, "Camila");
        Long intruder = verifiedUser("Intruso", "intruso@example.com");

        assertThat(tripService.listTrips(intruder)).isEmpty();
        assertThatThrownBy(() -> tripService.getTrip(trip.id(), intruder)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> settlementService.getBalances(trip.id(), intruder))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> expenseService.createExpense(trip.id(), new CreateExpenseRequest(
                "Hack", new BigDecimal("1000"), null, me, List.of(me)), intruder))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownTripIsNotFound() {
        assertThatThrownBy(() -> tripService.getTrip(999_999L, camila)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> settlementService.getBalances(999_999L, camila))
                .isInstanceOf(NotFoundException.class);
    }

    private Long verifiedUser(String name, String email) {
        User user = new User(name, email, "not-used-in-these-tests");
        user.markEmailVerified();
        return users.save(user).getId();
    }

    private static Long idOf(TripResponse trip, String name) {
        return trip.participants().stream()
                .filter(p -> p.name().equals(name))
                .findFirst().orElseThrow()
                .id();
    }

    private static BalanceResponse balanceOf(List<BalanceResponse> balances, String name) {
        return balances.stream()
                .filter(b -> b.participant().name().equals(name))
                .findFirst().orElseThrow();
    }
}