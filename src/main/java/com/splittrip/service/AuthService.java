package com.splittrip.service;

import com.splittrip.dto.AuthResponse;
import com.splittrip.dto.ForgotPasswordRequest;
import com.splittrip.dto.LoginRequest;
import com.splittrip.dto.MessageResponse;
import com.splittrip.dto.RegisterRequest;
import com.splittrip.dto.ResendVerificationRequest;
import com.splittrip.dto.ResetPasswordRequest;
import com.splittrip.dto.UserResponse;
import com.splittrip.dto.VerifyEmailRequest;
import com.splittrip.exception.ApiException;
import com.splittrip.exception.NotFoundException;
import com.splittrip.model.EmailVerificationToken;
import com.splittrip.model.PasswordResetToken;
import com.splittrip.model.User;
import com.splittrip.repository.EmailVerificationTokenRepository;
import com.splittrip.repository.PasswordResetTokenRepository;
import com.splittrip.repository.UserRepository;
import com.splittrip.security.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final Duration verificationExpiration;
    private final Duration passwordResetExpiration;

    public AuthService(UserRepository users,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       EmailService emailService,
                       @Value("${app.verification.expiration}") Duration verificationExpiration,
                       @Value("${app.password-reset.expiration}") Duration passwordResetExpiration) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.verificationExpiration = verificationExpiration;
        this.passwordResetExpiration = passwordResetExpiration;
    }

    public MessageResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        sendVerification(user);
        return new MessageResponse("Account created. Check your email to verify it.");
    }

    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = verificationTokens.findByToken(request.token())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid verification link"));
        if (token.isUsed() || token.isExpired(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification link has expired or was already used");
        }

        token.markUsed();
        token.getUser().markEmailVerified();
        return new MessageResponse("Email verified. You can now log in.");
    }

    /** Always answers the same, so nobody can find out which emails are registered. */
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        users.findByEmail(normalize(request.email()))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);
        return new MessageResponse("If the account exists and is not verified, a new link has been sent.");
    }

    /** Always answers the same, so nobody can find out which emails are registered. */
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        users.findByEmail(normalize(request.email())).ifPresent(user -> {
            String value = newToken();
            resetTokens.save(new PasswordResetToken(value, user, Instant.now().plus(passwordResetExpiration)));
            emailService.sendPasswordResetEmail(user, value);
        });
        return new MessageResponse("If the account exists, we sent a link to reset the password.");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokens.findByToken(request.token())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid reset link"));
        if (token.isUsed() || token.isExpired(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset link has expired or was already used");
        }

        token.markUsed();
        User user = token.getUser();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // Opening the link proves the user owns the email address.
        user.markEmailVerified();
        return new MessageResponse("Password updated. You can now log in.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Email is not verified");
        }

        return new AuthResponse(
                tokenService.issue(user, request.rememberMe()),
                "Bearer",
                tokenService.expiresInSeconds(request.rememberMe()),
                UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void sendVerification(User user) {
        String value = newToken();
        verificationTokens.save(new EmailVerificationToken(value, user, Instant.now().plus(verificationExpiration)));
        emailService.sendVerificationEmail(user, value);
    }

    /** 32 random bytes, URL-safe (43 characters). */
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}