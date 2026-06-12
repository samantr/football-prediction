package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService(null, null);

    @Test
    void exactScoreReturnsThreePointsInGroupStage() {
        Prediction prediction = prediction(2, 1, 2, 1, MatchStage.GROUP, MatchStatus.COMPLETED);

        assertThat(scoringService.calculateScore(prediction)).isEqualTo(3);
        assertThat(scoringService.classify(prediction)).isEqualTo(PredictionResultClassification.EXACT_SCORE);
    }

    @Test
    void correctWinnerReturnsOnePointInGroupStage() {
        Prediction prediction = prediction(1, 0, 2, 1, MatchStage.GROUP, MatchStatus.COMPLETED);

        assertThat(scoringService.calculateScore(prediction)).isEqualTo(1);
        assertThat(scoringService.classify(prediction)).isEqualTo(PredictionResultClassification.CORRECT_OUTCOME);
    }

    @Test
    void correctDrawReturnsOnePointInGroupStage() {
        Prediction prediction = prediction(0, 0, 1, 1, MatchStage.GROUP, MatchStatus.COMPLETED);

        assertThat(scoringService.calculateScore(prediction)).isEqualTo(1);
        assertThat(scoringService.classify(prediction)).isEqualTo(PredictionResultClassification.CORRECT_OUTCOME);
    }

    @Test
    void wrongPredictionReturnsZeroPointsInGroupStage() {
        Prediction prediction = prediction(1, 2, 2, 1, MatchStage.GROUP, MatchStatus.COMPLETED);

        assertThat(scoringService.calculateScore(prediction)).isZero();
        assertThat(scoringService.classify(prediction)).isEqualTo(PredictionResultClassification.WRONG);
    }

    @Test
    void exactScoreReturnsFourPointsInEliminationStage() {
        Prediction prediction = prediction(2, 1, 2, 1, MatchStage.ROUND_OF_16, MatchStatus.COMPLETED);

        assertThat(scoringService.calculateScore(prediction)).isEqualTo(4);
        assertThat(scoringService.classify(prediction)).isEqualTo(PredictionResultClassification.EXACT_SCORE);
    }

    @Test
    void correctWinnerAndDrawReturnTwoPointsInEliminationStage() {
        Prediction correctWinner = prediction(1, 0, 2, 1, MatchStage.FINAL, MatchStatus.COMPLETED);
        Prediction correctDraw = prediction(0, 0, 1, 1, MatchStage.SEMI_FINAL, MatchStatus.COMPLETED);

        assertThat(scoringService.calculateScore(correctWinner)).isEqualTo(2);
        assertThat(scoringService.classify(correctWinner)).isEqualTo(PredictionResultClassification.CORRECT_OUTCOME);
        assertThat(scoringService.calculateScore(correctDraw)).isEqualTo(2);
        assertThat(scoringService.classify(correctDraw)).isEqualTo(PredictionResultClassification.CORRECT_OUTCOME);
    }

    @Test
    void pendingMatchReturnsZeroPointsAndPendingClassification() {
        Prediction prediction = prediction(2, 1, null, null, MatchStage.GROUP, MatchStatus.SCHEDULED);

        assertThat(scoringService.calculateScore(prediction)).isZero();
        assertThat(scoringService.classify(prediction)).isEqualTo(PredictionResultClassification.PENDING);
    }

    @Test
    void leaderboardUsesStageAwarePointsAndClassificationCounts() {
        UserRepository userRepository = mock(UserRepository.class);
        PredictionRepository predictionRepository = mock(PredictionRepository.class);
        ScoringService service = new ScoringService(userRepository, predictionRepository);
        User user = user();
        Prediction exactElimination = prediction(2, 1, 2, 1, MatchStage.FINAL, MatchStatus.COMPLETED);
        Prediction outcomeElimination = prediction(1, 0, 2, 1, MatchStage.ROUND_OF_16, MatchStatus.COMPLETED);
        exactElimination.setUser(user);
        outcomeElimination.setUser(user);

        when(userRepository.findByRoleOrderByDisplayNameAsc(Role.USER)).thenReturn(List.of(user));
        when(predictionRepository.findAll()).thenReturn(List.of(exactElimination, outcomeElimination));

        List<LeaderboardEntry> leaderboard = service.leaderboard(null);

        assertThat(leaderboard).hasSize(1);
        assertThat(leaderboard.get(0).getTotalScore()).isEqualTo(6);
        assertThat(leaderboard.get(0).getExactScores()).isEqualTo(1);
        assertThat(leaderboard.get(0).getOutcomeScores()).isEqualTo(1);
        assertThat(leaderboard.get(0).getPredictionsCount()).isEqualTo(2);
    }

    private Prediction prediction(
            int predictedHome,
            int predictedAway,
            Integer actualHome,
            Integer actualAway,
            MatchStage stage,
            MatchStatus status
    ) {
        Match match = new Match();
        match.setStage(stage);
        match.setStatus(status);
        match.setHomeScore(actualHome);
        match.setAwayScore(actualAway);

        Prediction prediction = new Prediction();
        prediction.setMatch(match);
        prediction.setPredictedHomeScore(predictedHome);
        prediction.setPredictedAwayScore(predictedAway);
        return prediction;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDisplayName("User");
        user.setRole(Role.USER);
        return user;
    }
}
