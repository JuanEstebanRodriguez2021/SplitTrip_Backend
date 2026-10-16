package com.splittrip.security;

/**
 * Password rules: 8-72 characters (72 is BCrypt's limit), at least one uppercase letter,
 * one digit and one special character. Spaces don't count as special characters.
 */
public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,72}$";

    public static final String MESSAGE =
            "Password must be 8-72 characters and include an uppercase letter, a number and a special character";

    private PasswordPolicy() {
    }
}