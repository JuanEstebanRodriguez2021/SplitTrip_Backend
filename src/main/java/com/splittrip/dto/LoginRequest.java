package com.splittrip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** rememberMe = true issues a long-lived token (30 days). If omitted it is false. */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password, boolean rememberMe) {
}