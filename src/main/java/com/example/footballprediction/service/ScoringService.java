package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.TargetSide;
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

    public static final int GROUP_EXACT_SCORE_POINTS = 3;
    public static final int GROUP_CORRECT_OUTCOME_POINTS = 1;
    public static final int ELIMINATION_EXACT_SCORE_POINTS = 4;
    public static final int ELIMINATION_CORRECT_OUTCOME_POINTS = 2;

    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;

    public ScoringService(UserRepository userRepository, PredictionRepository predictionRepository) {
        this.userRepository = userRepository;
        this.predictionRepository = predictionRepository;
    }

    public int calculateScore(Prediction prediction) {
        PredictionResultClassification classification = classify(prediction);
        return scoreFor(prediction, classification);
    }

    public PredictionResultClassification classify(Prediction prediction) {
        if (prediction == null
                || prediction.getMatch() == null
                || prediction.getMatch().getStatus() != MatchStatus.COMPLETED
                || prediction.getMatch().getHomeScore() == null
                || prediction.getMatch().getAwayScore() == null
                || prediction.getPredictedHomeScore() == null
                || prediction.getPredictedAwayScore() == null) {
            return PredictionResultClassification.PENDING;
        }

        boolean exactScore = prediction.getPredictedHomeScore().equals(prediction.getMatch().getHomeScore())
                && prediction.getPredictedAwayScore().equals(prediction.getMatch().getAwayScore());
        if (exactScore) {
            return PredictionResultClassification.EXACT_SCORE;
        }

        if (!isGroupStage(prediction.getMatch())) {
            return classifyElimination(prediction);
        }

        int predictedOutcome = Integer.compare(prediction.getPredictedHomeScore(), prediction.getPredictedAwayScore());
        int actualOutcome = Integer.compare(prediction.getMatch().getHomeScore(), prediction.getMatch().getAwayScore());
        return predictedOutcome == actualOutcome
                ? PredictionResultClassification.CORRECT_OUTCOME
                : PredictionResultClassification.WRONG;
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

            PredictionResultClassification classification = classify(prediction);
            int points = scoreFor(prediction, classification);
            score.totalScore += points;
            score.predictionsCount++;
            if (classification == PredictionResultClassification.EXACT_SCORE) {
                score.exactScores++;
            } else if (classification == PredictionResultClassification.CORRECT_OUTCOME) {
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
                .map(this::toScoreRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PredictionScoreRow> adminReport(Long tournamentId) {
        List<Prediction> predictions = tournamentId == null
                ? predictionRepository.findAll()
                : predictionRepository.findByMatchTournamentIdOrderByUserDisplayNameAscMatchKickoffAtAsc(tournamentId);

        List<PredictionScoreRow> rows = new ArrayList<>();
        for (Prediction prediction : predictions) {
            rows.add(toScoreRow(prediction));
        }
        return rows;
    }

    public int totalScore(List<PredictionScoreRow> rows) {
        return rows.stream().mapToInt(PredictionScoreRow::getScore).sum();
    }

    private PredictionScoreRow toScoreRow(Prediction prediction) {
        PredictionResultClassification classification = classify(prediction);
        return new PredictionScoreRow(
                prediction.getUser(),
                prediction.getMatch(),
                prediction,
                scoreFor(prediction, classification),
                classification
        );
    }

    private int scoreFor(Prediction prediction, PredictionResultClassification classification) {
        if (classification == PredictionResultClassification.PENDING || isGroupStage(prediction.getMatch())) {
            return pointsFor(prediction, classification);
        }
        return calculateEliminationScore(prediction, classification);
    }

    private PredictionResultClassification classifyElimination(Prediction prediction) {
        Match match = prediction.getMatch();
        boolean actualTie = match.getHomeScore().equals(match.getAwayScore());
        boolean predictedTie = prediction.getPredictedHomeScore().equals(prediction.getPredictedAwayScore());

        if (!actualTie) {
            TargetSide actualWinner = winnerFromScore(match.getHomeScore(), match.getAwayScore());
            TargetSide predictedWinner = predictedTie
                    ? null
                    : winnerFromScore(prediction.getPredictedHomeScore(), prediction.getPredictedAwayScore());
            return actualWinner == predictedWinner
                    ? PredictionResultClassification.CORRECT_OUTCOME
                    : PredictionResultClassification.WRONG;
        }

        if (predictedTie) {
            return PredictionResultClassification.CORRECT_OUTCOME;
        }
        return PredictionResultClassification.WRONG;
    }

    private int calculateEliminationScore(Prediction prediction, PredictionResultClassification classification) {
        return switch (classification) {
            case EXACT_SCORE -> ELIMINATION_EXACT_SCORE_POINTS + penaltyWinnerBonus(prediction);
            case CORRECT_OUTCOME -> {
                if (actualScoreIsTied(prediction) && predictedScoreIsTied(prediction)) {
                    yield ELIMINATION_CORRECT_OUTCOME_POINTS + penaltyWinnerBonus(prediction);
                }
                yield ELIMINATION_CORRECT_OUTCOME_POINTS;
            }
            case WRONG, PENDING -> 0;
        };
    }

    private int penaltyWinnerBonus(Prediction prediction) {
        Match match = prediction.getMatch();
        if (!actualScoreIsTied(prediction) || match.getPenaltyWinner() == null) {
            return 0;
        }
        return match.getPenaltyWinner() == prediction.getPredictedPenaltyWinner()
                ? ELIMINATION_CORRECT_OUTCOME_POINTS
                : 0;
    }

    private boolean actualScoreIsTied(Prediction prediction) {
        return prediction.getMatch().getHomeScore().equals(prediction.getMatch().getAwayScore());
    }

    private boolean predictedScoreIsTied(Prediction prediction) {
        return prediction.getPredictedHomeScore().equals(prediction.getPredictedAwayScore());
    }

    private TargetSide winnerFromScore(Integer homeScore, Integer awayScore) {
        return homeScore > awayScore ? TargetSide.HOME : TargetSide.AWAY;
    }

    private int pointsFor(Prediction prediction, PredictionResultClassification classification) {
        return switch (classification) {
            case EXACT_SCORE -> isGroupStage(prediction.getMatch())
                    ? GROUP_EXACT_SCORE_POINTS
                    : ELIMINATION_EXACT_SCORE_POINTS;
            case CORRECT_OUTCOME -> isGroupStage(prediction.getMatch())
                    ? GROUP_CORRECT_OUTCOME_POINTS
                    : ELIMINATION_CORRECT_OUTCOME_POINTS;
            case WRONG, PENDING -> 0;
        };
    }

    private boolean isGroupStage(Match match) {
        return match == null || match.getStage() == null || match.getStage() == MatchStage.GROUP;
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
