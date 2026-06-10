package com.example.footballprediction.service;

public class LeaderboardEntry {

    private final Long userId;
    private final String displayName;
    private final String email;
    private final int totalScore;
    private final long exactScores;
    private final long outcomeScores;
    private final long predictionsCount;

    public LeaderboardEntry(
            Long userId,
            String displayName,
            String email,
            int totalScore,
            long exactScores,
            long outcomeScores,
            long predictionsCount
    ) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.totalScore = totalScore;
        this.exactScores = exactScores;
        this.outcomeScores = outcomeScores;
        this.predictionsCount = predictionsCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public long getExactScores() {
        return exactScores;
    }

    public long getOutcomeScores() {
        return outcomeScores;
    }

    public long getPredictionsCount() {
        return predictionsCount;
    }
}
