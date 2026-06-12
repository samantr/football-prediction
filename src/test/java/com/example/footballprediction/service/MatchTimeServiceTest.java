package com.example.footballprediction.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTimeServiceTest {

    private final MatchTimeService matchTimeService = new MatchTimeService();

    @Test
    void formatsStoredUtcLikeKickoffAsTurkiyeTime() {
        LocalDateTime storedUtcLikeKickoff = LocalDateTime.of(2026, 6, 11, 18, 0);

        assertThat(matchTimeService.formatTurkiye(storedUtcLikeKickoff))
                .isEqualTo("2026-06-11 21:00 TRT");
    }

    @Test
    void formatsAdminInputValueAsTurkiyeTime() {
        LocalDateTime storedUtcLikeKickoff = LocalDateTime.of(2026, 6, 11, 18, 0);

        assertThat(matchTimeService.formatTurkiyeInput(storedUtcLikeKickoff))
                .isEqualTo("2026-06-11T21:00");
    }

    @Test
    void parsesTurkiyeInputBackToStoredUtcLikeKickoff() {
        assertThat(matchTimeService.parseTurkiyeInputToStoredUtc("2026-06-11T21:00"))
                .isEqualTo(LocalDateTime.of(2026, 6, 11, 18, 0));
    }
}
