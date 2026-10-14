package com.splittrip.repository;

import com.splittrip.model.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** Loads expenses together with the payer and the split participants in one query. */
    @EntityGraph(attributePaths = {"paidBy", "splitAmong"})
    List<Expense> findByTripIdOrderByDateDescIdDesc(Long tripId);

    Optional<Expense> findByIdAndTripId(Long id, Long tripId);

    /** Returns null when the trip has no expenses. */
    @Query("select sum(e.amount) from Expense e where e.trip.id = :tripId")
    BigDecimal sumAmountByTripId(@Param("tripId") Long tripId);
}