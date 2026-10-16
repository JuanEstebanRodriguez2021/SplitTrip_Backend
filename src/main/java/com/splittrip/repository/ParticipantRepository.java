package com.splittrip.repository;

import com.splittrip.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByTripIdOrderByName(Long tripId);

    long countByTripId(Long tripId);

    boolean existsByTripIdAndNameIgnoreCase(Long tripId, String name);

    Optional<Participant> findByTripIdAndUserId(Long tripId, Long userId);
}