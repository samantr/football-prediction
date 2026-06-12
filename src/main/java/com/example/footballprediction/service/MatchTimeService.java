package com.example.footballprediction.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component("matchTimes")
public class MatchTimeService {

    private static final ZoneOffset STORED_ZONE = ZoneOffset.UTC;
    private static final ZoneId TURKIYE_ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public String formatTurkiye(LocalDateTime storedUtcLikeDateTime) {
        if (storedUtcLikeDateTime == null) {
            return "";
        }
        return toTurkiyeTime(storedUtcLikeDateTime).format(DISPLAY_FORMATTER) + " TRT";
    }

    public String formatTurkiyeInput(LocalDateTime storedUtcLikeDateTime) {
        if (storedUtcLikeDateTime == null) {
            return "";
        }
        return toTurkiyeTime(storedUtcLikeDateTime).format(INPUT_FORMATTER);
    }

    public LocalDateTime parseTurkiyeInputToStoredUtc(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Baslama zamani zorunludur.");
        }

        try {
            LocalDateTime turkiyeDateTime = LocalDateTime.parse(value.trim());
            return turkiyeDateTime.atZone(TURKIYE_ZONE).withZoneSameInstant(STORED_ZONE).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Baslama zamani gecersiz.", ex);
        }
    }

    private ZonedDateTime toTurkiyeTime(LocalDateTime storedUtcLikeDateTime) {
        return storedUtcLikeDateTime.atZone(STORED_ZONE).withZoneSameInstant(TURKIYE_ZONE);
    }
}
