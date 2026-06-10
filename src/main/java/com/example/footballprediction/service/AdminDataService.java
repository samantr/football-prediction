package com.example.footballprediction.service;

import com.example.footballprediction.domain.BracketRule;
import com.example.footballprediction.domain.BracketSourceType;
import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.TargetSide;
import com.example.footballprediction.domain.Team;
import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.repository.BracketRuleRepository;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.TeamRepository;
import com.example.footballprediction.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminDataService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final BracketRuleRepository bracketRuleRepository;

    public AdminDataService(
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            BracketRuleRepository bracketRuleRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.bracketRuleRepository = bracketRuleRepository;
    }

    @Transactional
    public Team saveTeam(Long id, Long tournamentId, String name, String code, String groupCode) {
        Tournament tournament = getTournament(tournamentId);
        Team team = id == null
                ? new Team()
                : teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found."));

        team.setTournament(tournament);
        team.setName(requireText(name, "Name"));
        team.setCode(requireText(code, "Code").toUpperCase());
        team.setGroupCode(blankToNullUpper(groupCode));
        return teamRepository.save(team);
    }

    @Transactional
    public Match saveMatch(
            Long id,
            Long tournamentId,
            Integer matchNo,
            MatchStage stage,
            String groupCode,
            Long homeTeamId,
            Long awayTeamId,
            String placeholderHome,
            String placeholderAway,
            LocalDateTime kickoffAt,
            MatchStatus status
    ) {
        if (matchNo == null || matchNo < 1) {
            throw new IllegalArgumentException("Match number must be positive.");
        }
        if (kickoffAt == null) {
            throw new IllegalArgumentException("Kickoff time is required.");
        }

        Tournament tournament = getTournament(tournamentId);
        Match match = id == null
                ? new Match()
                : matchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Match not found."));

        match.setTournament(tournament);
        match.setMatchNo(matchNo);
        match.setStage(stage == null ? MatchStage.GROUP : stage);
        match.setGroupCode(blankToNullUpper(groupCode));
        match.setHomeTeam(getOptionalTeam(homeTeamId, tournament.getId()));
        match.setAwayTeam(getOptionalTeam(awayTeamId, tournament.getId()));
        match.setPlaceholderHome(blankToNull(placeholderHome));
        match.setPlaceholderAway(blankToNull(placeholderAway));
        match.setKickoffAt(kickoffAt);
        match.setStatus(status == null ? MatchStatus.SCHEDULED : status);

        return matchRepository.save(match);
    }

    @Transactional
    public Match enterResult(Long matchId, Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null || homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores must be zero or greater.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found."));
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setStatus(MatchStatus.COMPLETED);
        return matchRepository.save(match);
    }

    @Transactional
    public BracketRule saveBracketRule(
            Long id,
            Long tournamentId,
            Long targetMatchId,
            TargetSide targetSide,
            BracketSourceType sourceType,
            String sourceValue
    ) {
        BracketRule rule = id == null
                ? new BracketRule()
                : bracketRuleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bracket rule not found."));

        Tournament tournament = getTournament(tournamentId);
        Match targetMatch = matchRepository.findById(targetMatchId)
                .orElseThrow(() -> new IllegalArgumentException("Target match not found."));

        if (!targetMatch.getTournament().getId().equals(tournament.getId())) {
            throw new IllegalArgumentException("Target match must belong to the selected tournament.");
        }

        rule.setTournament(tournament);
        rule.setTargetMatch(targetMatch);
        rule.setTargetSide(targetSide);
        rule.setSourceType(sourceType);
        rule.setSourceValue(requireText(sourceValue, "Source value").toUpperCase());
        return bracketRuleRepository.save(rule);
    }

    private Tournament getTournament(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("Tournament is required.");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found."));
    }

    private Team getOptionalTeam(Long teamId, Long tournamentId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found."));
        if (!team.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Team must belong to the selected tournament.");
        }
        return team;
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String blankToNullUpper(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().toUpperCase();
    }
}
