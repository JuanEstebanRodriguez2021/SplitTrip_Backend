package com.splittrip.service;

import com.splittrip.dto.AuthResponse;
import com.splittrip.dto.ForgotPasswordRequest;
import com.splittrip.dto.LoginRequest;
import com.splittrip.dto.RegisterRequest;
import com.splittrip.dto.ResendVerificationRequest;
import com.splittrip.dto.ResetPasswordRequest;
import com.splittrip.dto.VerifyEmailRequest;
import com.splittrip.exception.ApiException;
import com.splittrip.model.User;
import com.splittrip.repository.EmailVerificationTokenRepository;
import com.splittrip.repository.PasswordResetTokenRepository;
import com.splittrip.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    private static final String PASSWORD = "Secreta123!";
    private static final String NEW_PASSWORD = "NuevaClave9#";

    @Autowired AuthService authService;
    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository verificationTokens;
    @Autowired PasswordResetTokenRepository resetTokens;

    @Test
    void registerVerifyAndLogin() {
        authService.register(new RegisterRequest("Camila", "Camila@Example.com", PASSWORD));

        User user = users.findByEmail("camila@example.com").orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);

        assertThatThrownBy(() -> authService.login(new LoginRequest("camila@example.com", PASSWORD, false)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        authService.verifyEmail(new VerifyEmailRequest(latestVerificationToken(user)));

        AuthResponse response = authService.login(new LoginRequest("CAMILA@example.com", PASSWORD, false));
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().name()).isEqualTo("Camila");
        assertThat(authService.me(user.getId()).email()).isEqualTo("camila@example.com");
    }

    @Test
    void verificationLinkCanOnlyBeUsedOnce() {
        authService.register(new RegisterRequest("Luis", "luis@example.com", PASSWORD));
        String token = latestVerificationToken(users.findByEmail("luis@example.com").orElseThrow());

        authService.verifyEmail(new VerifyEmailRequest(token));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(token)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("made-up-token")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        authService.register(new RegisterRequest("Sara", "sara@example.com", PASSWORD));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Sara 2", "SARA@example.com", PASSWORD)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejectsWrongPasswordAndUnknownEmail() {
        registerVerified("Juan", "juan@example.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("juan@example.com", "Otra123!", false)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", PASSWORD, false)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void resendCreatesANewToken() {
        authService.register(new RegisterRequest("Diego", "diego@example.com", PASSWORD));
        User user = users.findByEmail("diego@example.com").orElseThrow();
        String first = latestVerificationToken(user);

        authService.resendVerification(new ResendVerificationRequest("diego@example.com"));

        assertThat(latestVerificationToken(user)).isNotEqualTo(first);
    }

    @Test
    void forgotAndResetPassword() {
        User user = registerVerified("Marta", "marta@example.com");

        // Unknown emails get the same answer and nothing breaks
        assertThat(authService.forgotPassword(new ForgotPasswordRequest("nobody@example.com")).message())
                .isEqualTo(authService.forgotPassword(new ForgotPasswordRequest("marta@example.com")).message());

        String token = latestResetToken(user);
        authService.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD));

        assertThatThrownBy(() -> authService.login(new LoginRequest("marta@example.com", PASSWORD, false)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThat(authService.login(new LoginRequest("marta@example.com", NEW_PASSWORD, false)).accessToken())
                .isNotBlank();

        // The link only works once
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(token, "OtraClave9#")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void resettingThePasswordAlsoVerifiesTheEmail() {
        authService.register(new RegisterRequest("Pablo", "pablo@example.com", PASSWORD));
        User user = users.findByEmail("pablo@example.com").orElseThrow();

        authService.forgotPassword(new ForgotPasswordRequest("pablo@example.com"));
        authService.resetPassword(new ResetPasswordRequest(latestResetToken(user), NEW_PASSWORD));

        assertThat(authService.login(new LoginRequest("pablo@example.com", NEW_PASSWORD, false)).accessToken())
                .isNotBlank();
    }

    @Test
    void rememberMeIssuesALongerToken() {
        registerVerified("Sofía", "sofia@example.com");

        long normal = authService.login(new LoginRequest("sofia@example.com", PASSWORD, false)).expiresIn();
        long remembered = authService.login(new LoginRequest("sofia@example.com", PASSWORD, true)).expiresIn();

        assertThat(normal).isEqualTo(24 * 60 * 60);
        assertThat(remembered).isEqualTo(30 * 24 * 60 * 60);
    }

    private User registerVerified(String name, String email) {
        authService.register(new RegisterRequest(name, email, PASSWORD));
        User user = users.findByEmail(email).orElseThrow();
        authService.verifyEmail(new VerifyEmailRequest(latestVerificationToken(user)));
        return user;
    }

    private String latestVerificationToken(User user) {
        return verificationTokens.findFirstByUserIdOrderByIdDesc(user.getId()).orElseThrow().getToken();
    }

    private String latestResetToken(User user) {
        return resetTokens.findFirstByUserIdOrderByIdDesc(user.getId()).orElseThrow().getToken();
    }
}