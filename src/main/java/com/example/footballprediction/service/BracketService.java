package com.example.footballprediction.service;

import com.example.footballprediction.domain.BracketRule;
import com.example.footballprediction.domain.BracketSourceType;
import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.TargetSide;
import com.example.footballprediction.domain.Team;
import com.example.footballprediction.repository.BracketRuleRepository;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BracketService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final BracketRuleRepository bracketRuleRepository;

    public BracketService(
            TournamentRepository tournamentRepository,
            MatchRepository matchRepository,
            BracketRuleRepository bracketRuleRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
        this.bracketRuleRepository = bracketRuleRepository;
    }

    @Transactional
    public BracketGenerationResult generateNextRound(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found."));

        List<String> messages = new ArrayList<>();
        boolean adminReviewRequired = false;
        List<BracketRule> rules = bracketRuleRepository.findByTournamentIdOrdered(tournamentId);

        if (rules.isEmpty()) {
            messages.add("No bracket rules have been configured for this tournament.");
            return new BracketGenerationResult(messages, false);
        }

        for (BracketRule rule : rules) {
            Resolution resolution = resolve(rule);
            Match targetMatch = rule.getTargetMatch();

            if (resolution.team() == null) {
                adminReviewRequired = true;
                targetMatch.setStatus(MatchStatus.ADMIN_REVIEW_REQUIRED);
                messages.add("Match " + targetMatch.getMatchNo() + " " + rule.getTargetSide()
                        + ": Admin review required. " + resolution.message());
                continue;
            }

            if (rule.getTargetSide() == TargetSide.HOME) {
                targetMatch.setHomeTeam(resolution.team());
                targetMatch.setPlaceholderHome(null);
            } else {
                targetMatch.setAwayTeam(resolution.team());
                targetMatch.setPlaceholderAway(null);
            }

            if (targetMatch.getStatus() == MatchStatus.ADMIN_REVIEW_REQUIRED
                    && targetMatch.getHomeTeam() != null
                    && targetMatch.getAwayTeam() != null
                    && targetMatch.getHomeScore() == null
                    && targetMatch.getAwayScore() == null) {
                targetMatch.setStatus(MatchStatus.SCHEDULED);
            }

            messages.add("Match " + targetMatch.getMatchNo() + " " + rule.getTargetSide()
                    + " filled with " + resolution.team().getName() + ".");
        }

        return new BracketGenerationResult(messages, adminReviewRequired);
    }

    private Resolution resolve(BracketRule rule) {
        if (rule.getSourceType() == BracketSourceType.GROUP_WINNER
                || rule.getSourceType() == BracketSourceType.GROUP_RUNNER_UP) {
            return resolveGroupSource(rule);
        }

        return resolveMatchSource(rule);
    }

    private Resolution resolveGroupSource(BracketRule rule) {
        String groupCode = rule.getSourceValue().trim().toUpperCase();
        List<Match> groupMatches = matchRepository.findByTournamentIdAndStageAndGroupCodeOrderByMatchNoAsc(
                rule.getTournament().getId(),
                MatchStage.GROUP,
                groupCode
        );

        if (groupMatches.isEmpty()) {
            return Resolution.review("No completed group matches were found for group " + groupCode + ".");
        }

        for (Match match : groupMatches) {
            if (match.getHomeTeam() == null
                    || match.getAwayTeam() == null
                    || match.getStatus() != MatchStatus.COMPLETED
                    || match.getHomeScore() == null
                    || match.getAwayScore() == null) {
                return Resolution.review("Group " + groupCode + " is not fully resulted.");
            }
        }

        Map<Long, Standing> standings = new HashMap<>();
        for (Match match : groupMatches) {
            standings.computeIfAbsent(match.getHomeTeam().getId(), id -> new Standing(match.getHomeTeam()));
            standings.computeIfAbsent(match.getAwayTeam().getId(), id -> new Standing(match.getAwayTeam()));
            applyResult(standings.get(match.getHomeTeam().getId()), standings.get(match.getAwayTeam().getId()), match);
        }

        List<Standing> sorted = standings.values().stream()
                .sorted(Comparator.comparingInt(Standing::points).reversed()
                        .thenComparing(Standing::goalDifference, Comparator.reverseOrder())
                        .thenComparing(Standing::goalsFor, Comparator.reverseOrder())
                        .thenComparing(standing -> standing.team().getName()))
                .toList();

        int position = rule.getSourceType() == BracketSourceType.GROUP_WINNER ? 0 : 1;
        if (sorted.size() <= position) {
            return Resolution.review("Group " + groupCode + " does not have enough teams.");
        }

        Standing selected = sorted.get(position);
        if (position > 0 && sameTieBreakRank(sorted.get(position - 1), selected)) {
            return Resolution.review("Tie-breaker data is not enough for group " + groupCode + ".");
        }
        if (position + 1 < sorted.size() && sameTieBreakRank(selected, sorted.get(position + 1))) {
            return Resolution.review("Tie-breaker data is not enough for group " + groupCode + ".");
        }

        return Resolution.resolved(selected.team());
    }

    private Resolution resolveMatchSource(BracketRule rule) {
        Integer sourceMatchNo;
        try {
            sourceMatchNo = Integer.valueOf(rule.getSourceValue().trim());
        } catch (NumberFormatException ex) {
            return Resolution.review("Source value for match winner/loser must be a match number.");
        }

        Match sourceMatch = matchRepository.findByTournamentIdAndMatchNo(rule.getTournament().getId(), sourceMatchNo)
                .orElse(null);
        if (sourceMatch == null) {
            return Resolution.review("Source match " + sourceMatchNo + " was not found.");
        }

        if (sourceMatch.getStatus() != MatchStatus.COMPLETED
                || sourceMatch.getHomeScore() == null
                || sourceMatch.getAwayScore() == null
                || sourceMatch.getHomeTeam() == null
                || sourceMatch.getAwayTeam() == null) {
            return Resolution.review("Source match " + sourceMatchNo + " is not fully resulted.");
        }

        if (sourceMatch.getHomeScore().equals(sourceMatch.getAwayScore())) {
            return Resolution.review("Source match " + sourceMatchNo + " is tied.");
        }

        boolean homeWon = sourceMatch.getHomeScore() > sourceMatch.getAwayScore();
        if (rule.getSourceType() == BracketSourceType.MATCH_WINNER) {
            return Resolution.resolved(homeWon ? sourceMatch.getHomeTeam() : sourceMatch.getAwayTeam());
        }
        return Resolution.resolved(homeWon ? sourceMatch.getAwayTeam() : sourceMatch.getHomeTeam());
    }

    private void applyResult(Standing home, Standing away, Match match) {
        home.goalsFor += match.getHomeScore();
        home.goalsAgainst += match.getAwayScore();
        away.goalsFor += match.getAwayScore();
        away.goalsAgainst += match.getHomeScore();

        int outcome = Integer.compare(match.getHomeScore(), match.getAwayScore());
        if (outcome > 0) {
            home.points += 3;
        } else if (outcome < 0) {
            away.points += 3;
        } else {
            home.points += 1;
            away.points += 1;
        }
    }

    private boolean sameTieBreakRank(Standing a, Standing b) {
        return a.points() == b.points()
                && a.goalDifference() == b.goalDifference()
                && a.goalsFor() == b.goalsFor();
    }

    private static class Standing {
        private final Team team;
        private int points;
        private int goalsFor;
        private int goalsAgainst;

        private Standing(Team team) {
            this.team = team;
        }

        private Team team() {
            return team;
        }

        private int points() {
            return points;
        }

        private int goalsFor() {
            return goalsFor;
        }

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }

    private record Resolution(Team team, String message) {
        private static Resolution resolved(Team team) {
            return new Resolution(team, "");
        }

        private static Resolution review(String message) {
            return new Resolution(null, message);
        }
    }
}
