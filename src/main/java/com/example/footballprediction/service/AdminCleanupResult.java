package com.example.footballprediction.service;

import java.util.List;

public record AdminCleanupResult(
        long predictionsDeleted,
        long bracketRulesDeleted,
        long matchesDeleted,
        long teamsDeleted,
        long tournamentsDeleted,
        long usersDeleted
) {

    public List<String> toMessages() {
        return List.of(
                "Tahminler silindi: " + predictionsDeleted,
                "Eşleşme kuralları silindi: " + bracketRulesDeleted,
                "Maçlar silindi: " + matchesDeleted,
                "Takımlar silindi: " + teamsDeleted,
                "Turnuvalar silindi: " + tournamentsDeleted,
                "Normal kullanıcılar silindi: " + usersDeleted
        );
    }
}
