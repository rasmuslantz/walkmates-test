package com.walkmates.lab1;

import com.walkmates.model.Seeker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lab 1, Part B — specification-based tests for {@link Seeker}.
 *
 * <p>Design your tests on paper first (equivalence partitions, boundary values, decision table)
 * from {@code docs/REQUIREMENTS.md} FR-1.1 / FR-1.3 / FR-1.2, then implement them here. One
 * worked example is provided; the {@code TODO}s are yours.</p>
 */
class SeekerSpecBasedTest {

    // ---- Worked example: boundary value at the maximum single top-up (FR-1.3) ----
    @Test
    @DisplayName("Top-up exactly at the 5000 SEK single-transaction maximum is accepted")
    void topUpAtSingleMaximumIsAccepted() {
        Seeker seeker = new Seeker("sam@example.com", "Sam", "0707654321");

        seeker.addFunds(Seeker.MAX_SINGLE_TOP_UP); // 5000.00, the boundary value

        assertThat(seeker.getBalance()).isEqualTo(Seeker.MAX_SINGLE_TOP_UP);
    }

    // ---- Equivalence Partitioning: registration fields (FR-1.1) ----

    @Test
    @DisplayName("Valid email is accepted")
    void validEmailIsAccepted() {
        Seeker seeker = new Seeker("sam@example.com", "Sam", "0707654321");

        assertThat(seeker.getEmail()).isEqualTo("sam@example.com");
    }

    @Test
    @DisplayName("Invalid email format is rejected")
    void invalidEmailFormatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("not-an-email", "Sam", "0707654321"));
    }

    @Test
    @DisplayName("Email longer than 254 characters is rejected")
    void emailAboveMaximumLengthIsRejected() {
        String email = "a".repeat(243) + "@example.com";

        assertThrows(IllegalArgumentException.class,
                () -> new Seeker(email, "Sam", "0707654321"));
    }

    @Test
    @DisplayName("Valid display name is accepted")
    void validDisplayNameIsAccepted() {
        Seeker seeker = new Seeker(
                "sam@example.com",
                "Anne-Marie O'Neil",
                "0707654321"
        );

        assertThat(seeker.getDisplayName()).isEqualTo("Anne-Marie O'Neil");
    }

    @Test
    @DisplayName("Display name shorter than 2 characters is rejected")
    void displayNameBelowMinimumLengthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", "A", "0707654321"));
    }

    @Test
    @DisplayName("Display name longer than 40 characters is rejected")
    void displayNameAboveMaximumLengthIsRejected() {
        String displayName = "A".repeat(41);

        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", displayName, "0707654321"));
    }

    @Test
    @DisplayName("Display name with invalid characters is rejected")
    void displayNameWithInvalidCharactersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", "Sam123", "0707654321"));
    }

    @Test
    @DisplayName("Valid Swedish phone number is accepted")
    void validSwedishPhoneNumberIsAccepted() {
        Seeker seeker = new Seeker("sam@example.com", "Sam", "0707654321");

        assertThat(seeker.getPhoneNumber()).isEqualTo("0707654321");
    }

    @Test
    @DisplayName("Valid international phone number is accepted")
    void validInternationalPhoneNumberIsAccepted() {
        Seeker seeker = new Seeker("sam@example.com", "Sam", "+4671234567");

        assertThat(seeker.getPhoneNumber()).isEqualTo("+4671234567");
    }

    @Test
    @DisplayName("Invalid phone number is rejected")
    void invalidPhoneNumberIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Seeker("sam@example.com", "Sam", "070123456"));
    }

    // TODO (BVA): just-below / at / just-above the 10.00 minimum top-up (FR-1.3).
    // TODO (BVA): a top-up that would push the balance above 20000.00 is rejected (FR-1.3).
    // TODO (Decision table): expected fee + max-bookings for each trust tier (FR-1.2).
}