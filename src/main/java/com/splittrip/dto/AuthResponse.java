package com.splittrip.dto;

/** Returned on login. Send accessToken as "Authorization: Bearer <token>". */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}