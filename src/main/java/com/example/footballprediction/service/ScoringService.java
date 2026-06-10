package com.example.footballprediction.service;

import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoringService {

    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;

    public ScoringService(UserRepository userRepository, PredictionRepository predictionRepository) {
        this.userRepository = userRepository;
        this.predictionRepository = predictionRepository;
    }

    public int calculateScore(Prediction prediction) {
        if (prediction == null
                || prediction.getMatch().getStatus() != MatchStatus.COMPLETED
                || prediction.getMatch().getHomeScore() == null
                || prediction.getMatch().getAwayScore() == null) {
            return 0;
        }

        boolean exactScore = prediction.getPredictedHomeScore().equals(prediction.getMatch().getHomeScore())
                && prediction.getPredictedAwayScore().equals(prediction.getMatch().getAwayScore());
        if (exactScore) {
            return 3;
        }

        int predictedOutcome = Integer.compare(prediction.getPredictedHomeScore(), prediction.getPredictedAwayScore());
        int actualOutcome = Integer.compare(prediction.getMatch().getHomeScore(), prediction.getMatch().getAwayScore());
        return predictedOutcome == actualOutcome ? 1 : 0;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(Long tournamentId) {
        List<User> users = userRepository.findByRoleOrderByDisplayNameAsc(Role.USER);
        List<Prediction> predictions = tournamentId == null
                ? predictionRepository.findAll()
                : predictionRepository.findByMatchTournamentIdOrderByUserDisplayNameAscMatchKickoffAtAsc(tournamentId);

        Map<Long, MutableScore> scores = new HashMap<>();
        for (User user : users) {
            scores.put(user.getId(), new MutableScore(user));
        }

        for (Prediction prediction : predictions) {
            MutableScore score = scores.get(prediction.getUser().getId());
            if (score == null) {
                continue;
            }

            int points = calculateScore(prediction);
            score.totalScore += points;
            score.predictionsCount++;
            if (points == 3) {
                score.exactScores++;
            } else if (points == 1) {
                score.outcomeScores++;
            }
        }

        return scores.values().stream()
                .map(MutableScore::toEntry)
                .sorted(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed()
                        .thenComparing(LeaderboardEntry::getExactScores, Comparator.reverseOrder())
                        .thenComparing(LeaderboardEntry::getDisplayName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PredictionScoreRow> userReport(Long userId, Long tournamentId) {
        List<Prediction> predictions = tournamentId == null
                ? predictionRepository.findByUserIdOrderByMatchKickoffAtAsc(userId)
                : predictionRepository.findByUserIdAndMatchTournamentIdOrderByMatchKickoffAtAsc(userId, tournamentId);

        return predictions.stream()
                .map(prediction -> new PredictionScoreRow(
                        prediction.getUser(),
                        prediction.getMatch(),
                        prediction,
                        calculateScore(prediction)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PredictionScoreRow> adminReport(Long tournamentId) {
        List<Prediction> predictions = tournamentId == null
                ? predictionRepository.findAll()
                : predictionRepository.findByMatchTournamentIdOrderByUserDisplayNameAscMatchKickoffAtAsc(tournamentId);

        List<PredictionScoreRow> rows = new ArrayList<>();
        for (Prediction prediction : predictions) {
            rows.add(new PredictionScoreRow(
                    prediction.getUser(),
                    prediction.getMatch(),
                    prediction,
                    calculateScore(prediction)
            ));
        }
        return rows;
    }

    public int totalScore(List<PredictionScoreRow> rows) {
        return rows.stream().mapToInt(PredictionScoreRow::getScore).sum();
    }

    private static class MutableScore {
        private final User user;
        private int totalScore;
        private long exactScores;
        private long outcomeScores;
        private long predictionsCount;

        private MutableScore(User user) {
            this.user = user;
        }

        private LeaderboardEntry toEntry() {
            return new LeaderboardEntry(
                    user.getId(),
                    user.getDisplayName(),
                    user.getEmail(),
                    totalScore,
                    exactScores,
                    outcomeScores,
                    predictionsCount
            );
        }
    }
}
