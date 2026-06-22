package com.example.footballprediction.service;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.Team;
import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.TeamRepository;
import com.example.footballprediction.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class FootballDataImportService {

    public static final String PROVIDER = "FOOTBALL_DATA";
    public static final String DEFAULT_COMPETITION_CODE = "WC";
    public static final int DEFAULT_SEASON = 2026;

    private final FootballDataClient footballDataClient;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;

    public FootballDataImportService(
            FootballDataClient footballDataClient,
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            PredictionRepository predictionRepository
    ) {
        this.footballDataClient = footballDataClient;
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.predictionRepository = predictionRepository;
    }

    public boolean isConfigured() {
        return footballDataClient.hasToken();
    }

    @Transactional
    public FootballDataImportResult syncTeams(Long tournamentId, String competitionCode, Integer season) {
        Tournament tournament = getTournament(tournamentId);
        FootballDataClient.TeamsResponse response = footballDataClient.fetchTeams(
                normalizeCompetitionCode(competitionCode),
                normalizeSeason(season)
        );
        TeamImportStats stats = new TeamImportStats();
        for (FootballDataClient.TeamResponse teamResponse : safeList(response == null ? null : response.teams())) {
            upsertTeam(tournament, teamResponse, stats);
        }
        return new FootballDataImportResult(stats.created, stats.updated, 0, 0, 0, 0, 0);
    }

    @Transactional
    public FootballDataImportResult syncMatches(Long tournamentId, String competitionCode, Integer season) {
        Tournament tournament = getTournament(tournamentId);
        FootballDataClient.MatchesResponse response = footballDataClient.fetchMatches(
                normalizeCompetitionCode(competitionCode),
                normalizeSeason(season)
        );
        return syncMatches(tournament, response, new TeamImportStats());
    }

    @Transactional
    public FootballDataImportResult syncAll(Long tournamentId, String competitionCode, Integer season) {
        Tournament tournament = getTournament(tournamentId);
        String normalizedCompetitionCode = normalizeCompetitionCode(competitionCode);
        int normalizedSeason = normalizeSeason(season);

        FootballDataClient.TeamsResponse teamsResponse = footballDataClient.fetchTeams(normalizedCompetitionCode, normalizedSeason);
        TeamImportStats teamStats = new TeamImportStats();
        for (FootballDataClient.TeamResponse teamResponse : safeList(teamsResponse == null ? null : teamsResponse.teams())) {
            upsertTeam(tournament, teamResponse, teamStats);
        }

        FootballDataClient.MatchesResponse matchesResponse = footballDataClient.fetchMatches(normalizedCompetitionCode, normalizedSeason);
        return syncMatches(tournament, matchesResponse, teamStats);
    }

    private FootballDataImportResult syncMatches(
            Tournament tournament,
            FootballDataClient.MatchesResponse response,
            TeamImportStats teamStats
    ) {
        int matchesCreated = 0;
        int matchesUpdated = 0;
        int matchesSkipped = 0;
        int resultsUpdated = 0;
        long preservedPredictions = 0;
        int nextMatchNo = matchRepository.findMaxMatchNoByTournamentId(tournament.getId()) + 1;
        Set<String> processedMatchExternalIds = new HashSet<>();

        List<FootballDataClient.MatchResponse> matches = safeList(response == null ? null : response.matches()).stream()
                .sorted(Comparator.comparing(FootballDataImportService::matchSortKey)
                        .thenComparingLong(match -> match.id() == null ? Long.MAX_VALUE : match.id()))
                .toList();

        for (FootballDataClient.MatchResponse matchResponse : matches) {
            if (matchResponse.id() == null) {
                matchesSkipped++;
                continue;
            }

            String externalId = String.valueOf(matchResponse.id());
            if (!processedMatchExternalIds.add(externalId)) {
                matchesSkipped++;
                continue;
            }

            Match match = matchRepository
                    .findByTournamentIdAndExternalProviderAndExternalId(tournament.getId(), PROVIDER, externalId)
                    .orElse(null);
            boolean created = match == null;
            if (created) {
                match = new Match();
                match.setTournament(tournament);
                match.setMatchNo(nextMatchNo++);
                match.setExternalProvider(PROVIDER);
                match.setExternalId(externalId);
            } else {
                match.setExternalProvider(PROVIDER);
                match.setExternalId(externalId);
                preservedPredictions += countPredictions(match);
            }

            LocalDateTime kickoffAt = parseKickoff(matchResponse.utcDate());
            if (kickoffAt == null && match.getKickoffAt() == null) {
                throw new IllegalStateException("API maçında başlama zamanı yok: " + externalId);
            }

            match.setStage(mapStage(matchResponse.stage()));
            match.setGroupCode(mapGroupCode(matchResponse.group()));
            if (kickoffAt != null) {
                match.setKickoffAt(kickoffAt);
            }
            match.setStatus(mapStatus(matchResponse.status()));
            match.setHomeTeam(upsertTeam(tournament, matchResponse.homeTeam(), teamStats));
            match.setAwayTeam(upsertTeam(tournament, matchResponse.awayTeam(), teamStats));
            match.setPlaceholderHome(match.getHomeTeam() == null ? placeholderName(matchResponse.homeTeam()) : null);
            match.setPlaceholderAway(match.getAwayTeam() == null ? placeholderName(matchResponse.awayTeam()) : null);

            FootballDataClient.FullTimeScoreResponse fullTime = matchResponse.score() == null
                    ? null
                    : matchResponse.score().fullTime();
            if (fullTime != null && fullTime.home() != null && fullTime.away() != null) {
                if (!Objects.equals(match.getHomeScore(), fullTime.home())
                        || !Objects.equals(match.getAwayScore(), fullTime.away())) {
                    resultsUpdated++;
                }
                match.setHomeScore(fullTime.home());
                match.setAwayScore(fullTime.away());
            }
            if (match.getStage() == MatchStage.GROUP
                    || (match.getHomeScore() != null
                    && match.getAwayScore() != null
                    && !match.getHomeScore().equals(match.getAwayScore()))) {
                match.setPenaltyWinner(null);
            }

            matchRepository.save(match);
            if (created) {
                matchesCreated++;
            } else {
                matchesUpdated++;
            }
        }

        return new FootballDataImportResult(
                teamStats.created,
                teamStats.updated,
                matchesCreated,
                matchesUpdated,
                matchesSkipped,
                resultsUpdated,
                preservedPredictions
        );
    }

    private long countPredictions(Match match) {
        return match.getId() == null ? 0 : predictionRepository.countByMatchId(match.getId());
    }

    private Team upsertTeam(Tournament tournament, FootballDataClient.MatchTeamResponse teamResponse, TeamImportStats stats) {
        if (teamResponse == null || teamResponse.id() == null) {
            return null;
        }
        return upsertTeam(
                tournament,
                new FootballDataClient.TeamResponse(teamResponse.id(), teamResponse.name(), teamResponse.shortName(), teamResponse.tla()),
                stats
        );
    }

    private Team upsertTeam(Tournament tournament, FootballDataClient.TeamResponse teamResponse, TeamImportStats stats) {
        if (teamResponse == null || teamResponse.id() == null) {
            return null;
        }

        String externalId = String.valueOf(teamResponse.id());
        String code = teamCode(teamResponse);
        Team team = teamRepository
                .findByTournamentIdAndExternalProviderAndExternalId(tournament.getId(), PROVIDER, externalId)
                .orElseGet(() -> teamRepository.findByTournamentIdAndCode(tournament.getId(), code).orElse(null));
        boolean created = team == null;
        if (created) {
            team = new Team();
            team.setTournament(tournament);
        }

        team.setName(truncate(firstText(teamResponse.name(), teamResponse.shortName(), code), 255));
        team.setCode(code);
        team.setExternalProvider(PROVIDER);
        team.setExternalId(externalId);

        Team saved = teamRepository.save(team);
        stats.record(externalId, created);
        return saved;
    }

    private Tournament getTournament(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("Turnuva zorunludur.");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnuva bulunamadı."));
    }

    private String normalizeCompetitionCode(String competitionCode) {
        String value = competitionCode == null ? "" : competitionCode.trim();
        return value.isEmpty() ? DEFAULT_COMPETITION_CODE : value.toUpperCase();
    }

    private int normalizeSeason(Integer season) {
        if (season == null) {
            return DEFAULT_SEASON;
        }
        if (season < 1900) {
            throw new IllegalArgumentException("Sezon geçerli bir yıl olmalıdır.");
        }
        return season;
    }

    private static String matchSortKey(FootballDataClient.MatchResponse match) {
        return match.utcDate() == null ? "" : match.utcDate();
    }

    private LocalDateTime parseKickoff(String utcDate) {
        if (utcDate == null || utcDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(utcDate.trim()), ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("API başlama zamanı okunamadı: " + utcDate, ex);
        }
    }

    private MatchStage mapStage(String stage) {
        if (stage == null) {
            return MatchStage.GROUP;
        }
        return switch (stage.trim().toUpperCase()) {
            case "LAST_16", "ROUND_OF_16" -> MatchStage.ROUND_OF_16;
            case "QUARTER_FINALS", "QUARTER_FINAL" -> MatchStage.QUARTER_FINAL;
            case "SEMI_FINALS", "SEMI_FINAL" -> MatchStage.SEMI_FINAL;
            case "THIRD_PLACE" -> MatchStage.THIRD_PLACE;
            case "FINAL" -> MatchStage.FINAL;
            default -> MatchStage.GROUP;
        };
    }

    private MatchStatus mapStatus(String status) {
        if (status != null && status.trim().equalsIgnoreCase("FINISHED")) {
            return MatchStatus.COMPLETED;
        }
        return MatchStatus.SCHEDULED;
    }

    private String mapGroupCode(String group) {
        if (group == null || group.trim().isEmpty()) {
            return null;
        }
        String value = group.trim().toUpperCase();
        if (value.startsWith("GROUP_")) {
            return truncate(value.substring("GROUP_".length()), 50);
        }
        if (value.startsWith("GROUP ")) {
            return truncate(value.substring("GROUP ".length()), 50);
        }
        return truncate(value, 50);
    }

    private String placeholderName(FootballDataClient.MatchTeamResponse teamResponse) {
        if (teamResponse == null) {
            return null;
        }
        String name = firstText(teamResponse.name(), teamResponse.shortName(), null);
        if (name == null || name.equalsIgnoreCase("TBD")) {
            return null;
        }
        return truncate(name, 255);
    }

    private String teamCode(FootballDataClient.TeamResponse teamResponse) {
        String value = firstText(teamResponse.tla(), teamResponse.shortName(), teamResponse.name());
        if (value == null) {
            value = String.valueOf(teamResponse.id());
        }
        return truncate(value.trim().toUpperCase().replaceAll("\\s+", "_"), 50);
    }

    private String firstText(String first, String second, String third) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        if (third != null && !third.trim().isEmpty()) {
            return third.trim();
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static class TeamImportStats {
        private final Set<String> countedExternalIds = new HashSet<>();
        private int created;
        private int updated;

        private void record(String externalId, boolean createdTeam) {
            if (!countedExternalIds.add(externalId)) {
                return;
            }
            if (createdTeam) {
                created++;
            } else {
                updated++;
            }
        }
    }
}
