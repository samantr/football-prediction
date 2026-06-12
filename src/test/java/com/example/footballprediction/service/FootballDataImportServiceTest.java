package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.TeamRepository;
import com.example.footballprediction.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FootballDataImportServiceTest {

    @Mock
    private FootballDataClient footballDataClient;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Test
    void importingExistingExternalMatchUpdatesSameMatchAndPreservesPredictionData() {
        Tournament tournament = tournament();
        Match existingMatch = importedMatch(tournament);
        Prediction prediction = prediction(existingMatch);
        FootballDataImportService service = service();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findMaxMatchNoByTournamentId(1L)).thenReturn(7);
        when(footballDataClient.fetchMatches("WC", 2026))
                .thenReturn(new FootballDataClient.MatchesResponse(List.of(apiMatch(99L, "FINISHED", 2, 1))));
        when(matchRepository.findByTournamentIdAndExternalProviderAndExternalId(1L, FootballDataImportService.PROVIDER, "99"))
                .thenReturn(Optional.of(existingMatch));
        when(predictionRepository.countByMatchId(10L)).thenReturn(1L);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FootballDataImportResult result = service.syncMatches(1L, "WC", 2026);

        ArgumentCaptor<Match> savedMatch = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(savedMatch.capture());
        assertThat(savedMatch.getValue()).isSameAs(existingMatch);
        assertThat(savedMatch.getValue().getId()).isEqualTo(10L);
        assertThat(savedMatch.getValue().getMatchNo()).isEqualTo(7);
        assertThat(savedMatch.getValue().getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(savedMatch.getValue().getHomeScore()).isEqualTo(2);
        assertThat(savedMatch.getValue().getAwayScore()).isEqualTo(1);
        assertThat(savedMatch.getValue().getKickoffAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 18, 0));
        assertThat(prediction.getMatch()).isSameAs(existingMatch);
        assertThat(prediction.getPredictedHomeScore()).isEqualTo(1);
        assertThat(prediction.getPredictedAwayScore()).isEqualTo(0);
        assertThat(result.matchesCreated()).isZero();
        assertThat(result.matchesUpdated()).isEqualTo(1);
        assertThat(result.resultsUpdated()).isEqualTo(1);
        assertThat(result.preservedPredictions()).isEqualTo(1);

        verify(predictionRepository).countByMatchId(10L);
        verifyNoMoreInteractions(predictionRepository);
    }

    @Test
    void repeatedImportUpdatesExistingMatchInsteadOfCreatingDuplicate() {
        Tournament tournament = tournament();
        Match createdMatch = new Match();
        FootballDataImportService service = service();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament), Optional.of(tournament));
        when(matchRepository.findMaxMatchNoByTournamentId(1L)).thenReturn(0, 1);
        when(footballDataClient.fetchMatches("WC", 2026))
                .thenReturn(new FootballDataClient.MatchesResponse(List.of(apiMatch(99L, "SCHEDULED", null, null))))
                .thenReturn(new FootballDataClient.MatchesResponse(List.of(apiMatch(99L, "FINISHED", 3, 2))));
        when(matchRepository.findByTournamentIdAndExternalProviderAndExternalId(1L, FootballDataImportService.PROVIDER, "99"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(10L);
            createdMatch.setId(match.getId());
            createdMatch.setTournament(match.getTournament());
            createdMatch.setMatchNo(match.getMatchNo());
            createdMatch.setExternalProvider(match.getExternalProvider());
            createdMatch.setExternalId(match.getExternalId());
            return match;
        });

        FootballDataImportResult firstImport = service.syncMatches(1L, "WC", 2026);
        FootballDataImportResult secondImport = service.syncMatches(1L, "WC", 2026);

        assertThat(firstImport.matchesCreated()).isEqualTo(1);
        assertThat(firstImport.matchesUpdated()).isZero();
        assertThat(secondImport.matchesCreated()).isZero();
        assertThat(secondImport.matchesUpdated()).isEqualTo(1);
        assertThat(createdMatch.getId()).isEqualTo(10L);
        verify(matchRepository, never()).delete(any(Match.class));
    }

    @Test
    void duplicateExternalMatchIdsInOneApiPayloadAreSkipped() {
        Tournament tournament = tournament();
        FootballDataImportService service = service();

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findMaxMatchNoByTournamentId(1L)).thenReturn(0);
        when(footballDataClient.fetchMatches("WC", 2026))
                .thenReturn(new FootballDataClient.MatchesResponse(List.of(
                        apiMatch(99L, "SCHEDULED", null, null),
                        apiMatch(99L, "FINISHED", 2, 1)
                )));
        when(matchRepository.findByTournamentIdAndExternalProviderAndExternalId(1L, FootballDataImportService.PROVIDER, "99"))
                .thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FootballDataImportResult result = service.syncMatches(1L, "WC", 2026);

        assertThat(result.matchesCreated()).isEqualTo(1);
        assertThat(result.matchesUpdated()).isZero();
        assertThat(result.matchesSkipped()).isEqualTo(1);
        verify(matchRepository).save(any(Match.class));
        verifyNoMoreInteractions(predictionRepository);
    }

    private FootballDataImportService service() {
        return new FootballDataImportService(
                footballDataClient,
                tournamentRepository,
                teamRepository,
                matchRepository,
                predictionRepository
        );
    }

    private Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("World Cup");
        tournament.setSeason("2026");
        return tournament;
    }

    private Match importedMatch(Tournament tournament) {
        Match match = new Match();
        match.setId(10L);
        match.setTournament(tournament);
        match.setMatchNo(7);
        match.setStage(MatchStage.GROUP);
        match.setKickoffAt(LocalDateTime.of(2026, 6, 11, 17, 0));
        match.setStatus(MatchStatus.SCHEDULED);
        match.setExternalProvider(FootballDataImportService.PROVIDER);
        match.setExternalId("99");
        return match;
    }

    private Prediction prediction(Match match) {
        Prediction prediction = new Prediction();
        prediction.setId(100L);
        prediction.setUser(new User());
        prediction.setMatch(match);
        prediction.setPredictedHomeScore(1);
        prediction.setPredictedAwayScore(0);
        return prediction;
    }

    private FootballDataClient.MatchResponse apiMatch(Long id, String status, Integer homeScore, Integer awayScore) {
        FootballDataClient.FullTimeScoreResponse fullTime = homeScore == null || awayScore == null
                ? null
                : new FootballDataClient.FullTimeScoreResponse(homeScore, awayScore);
        return new FootballDataClient.MatchResponse(
                id,
                "2026-06-11T18:00:00Z",
                status,
                1,
                "GROUP_STAGE",
                "GROUP_A",
                null,
                null,
                new FootballDataClient.ScoreResponse(fullTime)
        );
    }
}
