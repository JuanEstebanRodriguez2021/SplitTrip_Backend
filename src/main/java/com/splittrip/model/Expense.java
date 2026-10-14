package com.splittrip.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An expense paid by one participant and split evenly among the given
 * participants (who may or may not include the payer).
 */
@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String description;

    /** Always BigDecimal for money, never double. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_id")
    private Participant paidBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "expense_participant",
            joinColumns = @JoinColumn(name = "expense_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id"))
    private List<Participant> splitAmong = new ArrayList<>();

    protected Expense() {
        // required by JPA
    }

    public Expense(String description, BigDecimal amount, LocalDate date, Trip trip,
                   Participant paidBy, List<Participant> splitAmong) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.trip = trip;
        this.paidBy = paidBy;
        this.splitAmong = new ArrayList<>(splitAmong);
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public Trip getTrip() {
        return trip;
    }

    public Participant getPaidBy() {
        return paidBy;
    }

    public List<Participant> getSplitAmong() {
        return splitAmong;
    }
}