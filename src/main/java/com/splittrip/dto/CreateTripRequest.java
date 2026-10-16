package com.splittrip.dto;

import com.splittrip.model.TripCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The creator is always added as a participant automatically, so {@code participants}
 * only needs the other people. If {@code category} is omitted, TRIP is used.
 */
public record CreateTripRequest(
        @NotBlank @Size(max = 100) String name,
        TripCategory category,
        @Size(max = 50) List<@NotBlank @Size(max = 60) String> participants) {
}