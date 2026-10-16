package com.splittrip.security;

import com.splittrip.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Issues signed JWT access tokens. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration expiration;
    private final Duration rememberMeExpiration;

    public TokenService(JwtEncoder encoder,
                        @Value("${app.jwt.expiration}") Duration expiration,
                        @Value("${app.jwt.remember-me-expiration}") Duration rememberMeExpiration) {
        this.encoder = encoder;
        this.expiration = expiration;
        this.rememberMeExpiration = rememberMeExpiration;
    }

    public String issue(User user, boolean rememberMe) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("splittrip")
                .issuedAt(now)
                .expiresAt(now.plus(ttl(rememberMe)))
                .subject(String.valueOf(user.getId()))
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds(boolean rememberMe) {
        return ttl(rememberMe).toSeconds();
    }

    private Duration ttl(boolean rememberMe) {
        return rememberMe ? rememberMeExpiration : expiration;
    }
}