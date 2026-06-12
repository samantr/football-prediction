package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.User;

public class PredictionScoreRow {

    private final User user;
    private final Match match;
    private final Prediction prediction;
    private final int score;
    private final PredictionResultClassification classification;

    public PredictionScoreRow(
            User user,
            Match match,
            Prediction prediction,
            int score,
            PredictionResultClassification classification
    ) {
        this.user = user;
        this.match = match;
        this.prediction = prediction;
        this.score = score;
        this.classification = classification;
    }

    public User getUser() {
        return user;
    }

    public Match getMatch() {
        return match;
    }

    public Prediction getPrediction() {
        return prediction;
    }

    public int getScore() {
        return score;
    }

    public PredictionResultClassification getClassification() {
        return classification;
    }
}
