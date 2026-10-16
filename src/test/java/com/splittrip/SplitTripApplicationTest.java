package com.splittrip;

import com.splittrip.model.Expense;
import com.splittrip.model.Participant;
import com.splittrip.model.Trip;
import com.splittrip.model.TripCategory;
import com.splittrip.model.User;
import com.splittrip.repository.ExpenseRepository;
import com.splittrip.repository.ParticipantRepository;
import com.splittrip.repository.TripRepository;
import com.splittrip.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that the app starts and the model is saved and read correctly in H2. */
@SpringBootTest
@Transactional
class SplitTripApplicationTest {

    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired ParticipantRepository participants;
    @Autowired ExpenseRepository expenses;

    @Test
    void savesAndReadsATripWithExpenses() {
        User owner = users.save(new User("Ana", "ana@example.com", "hash"));
        Trip trip = trips.save(new Trip("Santa Marta 2026", TripCategory.TRIP, owner));
        Participant ana = participants.save(new Participant("Ana", trip, owner));
        Participant luis = participants.save(new Participant("Luis", trip));

        expenses.save(new Expense("Hotel", new BigDecimal("320000"), LocalDate.of(2026, 10, 10),
                trip, ana, List.of(ana, luis)));

        assertThat(participants.findByTripIdOrderByName(trip.getId()))
                .extracting(Participant::getName)
                .containsExactly("Ana", "Luis");

        List<Expense> loaded = expenses.findByTripIdOrderByDateDescIdDesc(trip.getId());
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getPaidBy().getName()).isEqualTo("Ana");
        assertThat(loaded.get(0).getSplitAmong()).hasSize(2);
        assertThat(trips.findByOwnerIdOrderByIdDesc(owner.getId())).hasSize(1);
    }
}