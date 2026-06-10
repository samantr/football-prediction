package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public PredictionService(
            PredictionRepository predictionRepository,
            MatchRepository matchRepository,
            UserRepository userRepository
    ) {
        this.predictionRepository = predictionRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    public boolean canEditPrediction(Match match) {
        return match.getKickoffAt() != null
                && LocalDateTime.now().isBefore(match.getKickoffAt().minusHours(1));
    }

    @Transactional
    public Prediction savePrediction(Long userId, Long matchId, Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null || homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Predicted scores must be zero or greater.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found."));

        if (!canEditPrediction(match)) {
            throw new IllegalStateException("Prediction editing is closed for this match.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Prediction prediction = predictionRepository.findByUserIdAndMatchId(userId, matchId)
                .orElseGet(Prediction::new);
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setPredictedHomeScore(homeScore);
        prediction.setPredictedAwayScore(awayScore);
        return predictionRepository.save(prediction);
    }

    @Transactional(readOnly = true)
    public Map<Long, Prediction> findPredictionMapForUser(Long userId, Long tournamentId) {
        List<Prediction> predictions = tournamentId == null
                ? predictionRepository.findByUserIdOrderByMatchKickoffAtAsc(userId)
                : predictionRepository.findByUserIdAndMatchTournamentIdOrderByMatchKickoffAtAsc(userId, tournamentId);

        return predictions.stream()
                .collect(Collectors.toMap(prediction -> prediction.getMatch().getId(), prediction -> prediction));
    }
}
