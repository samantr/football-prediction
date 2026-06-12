package com.example.footballprediction.service;

import java.util.List;

public record FootballDataImportResult(
        int teamsCreated,
        int teamsUpdated,
        int matchesCreated,
        int matchesUpdated,
        int matchesSkipped,
        int resultsUpdated,
        long preservedPredictions
) {

    public List<String> toMessages() {
        return List.of(
                "Takımlar: " + teamsCreated + " yeni, " + teamsUpdated + " güncellendi.",
                "Maçlar: " + matchesCreated + " yeni, " + matchesUpdated + " güncellendi, " + matchesSkipped + " atlandı.",
                "Sonuçlar: " + resultsUpdated + " güncellendi.",
                "Korunan tahminler: " + preservedPredictions + "."
        );
    }
}
