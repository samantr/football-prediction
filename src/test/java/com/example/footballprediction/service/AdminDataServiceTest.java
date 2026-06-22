package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.TargetSide;
import com.example.footballprediction.repository.BracketRuleRepository;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.TeamRepository;
import com.example.footballprediction.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDataServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private BracketRuleRepository bracketRuleRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @InjectMocks
    private AdminDataService adminDataService;

    @Test
    void tiedEliminationResultRequiresPenaltyWinner() {
        Match match = match(MatchStage.FINAL);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> adminDataService.enterResult(10L, 1, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("penalt");

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void penaltyWinnerIsStoredOnlyForTiedEliminationResults() {
        Match match = match(MatchStage.FINAL);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match tiedResult = adminDataService.enterResult(10L, 1, 1, TargetSide.HOME);
        assertThat(tiedResult.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(tiedResult.getPenaltyWinner()).isEqualTo(TargetSide.HOME);

        Match nonTiedResult = adminDataService.enterResult(10L, 2, 1, TargetSide.AWAY);
        assertThat(nonTiedResult.getPenaltyWinner()).isNull();
    }

    private Match match(MatchStage stage) {
        Match match = new Match();
        match.setId(10L);
        match.setStage(stage);
        match.setStatus(MatchStatus.SCHEDULED);
        return match;
    }
}
