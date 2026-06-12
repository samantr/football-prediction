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
import java.time.ZoneOffset;
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
                && LocalDateTime.now(ZoneOffset.UTC).isBefore(match.getKickoffAt().minusHours(1));
    }

    @Transactional
    public Prediction savePrediction(Long userId, Long matchId, Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null || homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Tahmin skorları sıfır veya daha büyük olmalıdır.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Maç bulunamadı."));

        if (!canEditPrediction(match)) {
            throw new IllegalStateException("Tahmin süresi doldu.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        Prediction prediction = predictionRepository.findByUserIdAndMatchId(userId, matchId)
                .orElseGet(Prediction::new);
        requireOwner(userId, prediction);
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

    @Transactional
    public void deletePrediction(Long currentUserId, Long predictionId, boolean admin) {
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("Tahmin bulunamadı."));

        if (!admin) {
            requireOwner(currentUserId, prediction);
            if (!canEditPrediction(prediction.getMatch())) {
                throw new IllegalStateException("Tahmin süresi doldu.");
            }
        }

        predictionRepository.delete(prediction);
    }

    private void requireOwner(Long userId, Prediction prediction) {
        if (prediction.getId() == null) {
            return;
        }
        if (prediction.getUser() == null || !userId.equals(prediction.getUser().getId())) {
            throw new IllegalStateException("Bu tahmini düzenleme yetkiniz yok.");
        }
    }
}
