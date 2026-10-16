package com.splittrip.repository;

import com.splittrip.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByOwnerIdOrderByIdDesc(Long ownerId);

    Optional<Trip> findByIdAndOwnerId(Long id, Long ownerId);
}
