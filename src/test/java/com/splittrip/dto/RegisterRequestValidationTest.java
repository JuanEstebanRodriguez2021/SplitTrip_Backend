package com.splittrip.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAStrongPassword() {
        assertThat(violations(new RegisterRequest("Camila", "camila@example.com", "Secreta123!"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Sec12!",       // too short
            "secreta123!",  // no uppercase
            "Secreta!!!",   // no number
            "Secreta123",   // no special character
            "Secreta 123"   // a space is not a special character
    })
    void rejectsWeakPasswords(String password) {
        assertThat(violations(new RegisterRequest("Camila", "camila@example.com", password)))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("password");
    }

    @Test
    void rejectsAnInvalidEmail() {
        assertThat(violations(new RegisterRequest("Camila", "not-an-email", "Secreta123!")))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("email");
    }

    private static Set<ConstraintViolation<RegisterRequest>> violations(RegisterRequest request) {
        return VALIDATOR.validate(request);
    }
}