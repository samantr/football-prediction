package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PredictionService predictionService;

    @Test
    void rejectsNegativeScoresBeforeSaving() {
        assertThatThrownBy(() -> predictionService.savePrediction(1L, 10L, -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tahmin skorları sıfır veya daha büyük olmalıdır.");

        verifyNoInteractions(matchRepository, userRepository, predictionRepository);
    }

    @Test
    void blocksPredictionWhenKickoffIsLessThanOneHourAway() {
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> predictionService.savePrediction(1L, 10L, 1, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tahmin süresi doldu.");

        verify(predictionRepository, never()).save(any(Prediction.class));
    }

    @Test
    void savesPredictionForAuthenticatedUserOnly() {
        User user = user(1L);
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusHours(2));
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(predictionRepository.findByUserIdAndMatchId(1L, 10L)).thenReturn(Optional.empty());
        when(predictionRepository.save(any(Prediction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prediction saved = predictionService.savePrediction(1L, 10L, 2, 1);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getMatch()).isSameAs(match);
        assertThat(saved.getPredictedHomeScore()).isEqualTo(2);
        assertThat(saved.getPredictedAwayScore()).isEqualTo(1);
        verify(predictionRepository).findByUserIdAndMatchId(1L, 10L);

        ArgumentCaptor<Prediction> predictionCaptor = ArgumentCaptor.forClass(Prediction.class);
        verify(predictionRepository).save(predictionCaptor.capture());
        assertThat(predictionCaptor.getValue().getUser().getId()).isEqualTo(1L);
    }

    @Test
    void editsOwnPredictionBeforeDeadline() {
        User user = user(1L);
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusHours(2));
        Prediction existingPrediction = prediction(20L, user, match, 0, 0);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(predictionRepository.findByUserIdAndMatchId(1L, 10L)).thenReturn(Optional.of(existingPrediction));
        when(predictionRepository.save(any(Prediction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prediction saved = predictionService.savePrediction(1L, 10L, 2, 1);

        assertThat(saved).isSameAs(existingPrediction);
        assertThat(saved.getPredictedHomeScore()).isEqualTo(2);
        assertThat(saved.getPredictedAwayScore()).isEqualTo(1);
        verify(predictionRepository).save(existingPrediction);
    }

    @Test
    void rejectsEditingPredictionOwnedByAnotherUser() {
        User currentUser = user(1L);
        User otherUser = user(2L);
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusHours(2));
        Prediction otherPrediction = prediction(20L, otherUser, match, 0, 0);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(predictionRepository.findByUserIdAndMatchId(1L, 10L)).thenReturn(Optional.of(otherPrediction));

        assertThatThrownBy(() -> predictionService.savePrediction(1L, 10L, 2, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yetkiniz");

        verify(predictionRepository, never()).save(any(Prediction.class));
    }

    @Test
    void rejectsDeletingPredictionOwnedByAnotherUser() {
        User otherUser = user(2L);
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusHours(2));
        Prediction otherPrediction = prediction(20L, otherUser, match, 0, 0);
        when(predictionRepository.findById(20L)).thenReturn(Optional.of(otherPrediction));

        assertThatThrownBy(() -> predictionService.deletePrediction(1L, 20L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yetkiniz");

        verify(predictionRepository, never()).delete(any(Prediction.class));
    }

    @Test
    void rejectsDeletingOwnPredictionAfterDeadline() {
        User user = user(1L);
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        Prediction prediction = prediction(20L, user, match, 0, 0);
        when(predictionRepository.findById(20L)).thenReturn(Optional.of(prediction));

        assertThatThrownBy(() -> predictionService.deletePrediction(1L, 20L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tahmin süresi doldu.");

        verify(predictionRepository, never()).delete(any(Prediction.class));
    }

    @Test
    void adminCanDeleteAnotherUsersPrediction() {
        User otherUser = user(2L);
        Match match = match(10L, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        Prediction otherPrediction = prediction(20L, otherUser, match, 0, 0);
        when(predictionRepository.findById(20L)).thenReturn(Optional.of(otherPrediction));

        predictionService.deletePrediction(1L, 20L, true);

        verify(predictionRepository).delete(otherPrediction);
    }

    private Match match(Long id, LocalDateTime kickoffAt) {
        Match match = new Match();
        match.setId(id);
        match.setKickoffAt(kickoffAt);
        return match;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("User");
        user.setRole(Role.USER);
        return user;
    }

    private Prediction prediction(Long id, User user, Match match, int predictedHome, int predictedAway) {
        Prediction prediction = new Prediction();
        prediction.setId(id);
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setPredictedHomeScore(predictedHome);
        prediction.setPredictedAwayScore(predictedAway);
        return prediction;
    }
}
