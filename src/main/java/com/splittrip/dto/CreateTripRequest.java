package com.splittrip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTripRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) List<@NotBlank @Size(max = 60) String> participants) {
}