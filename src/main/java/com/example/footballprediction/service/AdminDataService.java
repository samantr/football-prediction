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
import com.example.footballprediction.repository.PredictionRepository;
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
    private final PredictionRepository predictionRepository;

    public AdminDataService(
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            BracketRuleRepository bracketRuleRepository,
            PredictionRepository predictionRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.bracketRuleRepository = bracketRuleRepository;
        this.predictionRepository = predictionRepository;
    }

    @Transactional
    public Team saveTeam(Long id, Long tournamentId, String name, String code, String groupCode) {
        Tournament tournament = getTournament(tournamentId);
        Team team = id == null
                ? new Team()
                : teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı."));

        team.setTournament(tournament);
        team.setName(requireText(name, "Ad"));
        team.setCode(requireText(code, "Kod").toUpperCase());
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
            throw new IllegalArgumentException("Maç numarası pozitif olmalıdır.");
        }
        if (kickoffAt == null) {
            throw new IllegalArgumentException("Başlama zamanı zorunludur.");
        }

        Tournament tournament = getTournament(tournamentId);
        Match match = id == null
                ? new Match()
                : matchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Maç bulunamadı."));

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
            throw new IllegalArgumentException("Skorlar sıfır veya daha büyük olmalıdır.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Maç bulunamadı."));
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
                : bracketRuleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Eşleşme kuralı bulunamadı."));

        Tournament tournament = getTournament(tournamentId);
        Match targetMatch = matchRepository.findById(targetMatchId)
                .orElseThrow(() -> new IllegalArgumentException("Hedef maç bulunamadı."));

        if (!targetMatch.getTournament().getId().equals(tournament.getId())) {
            throw new IllegalArgumentException("Hedef maç seçili turnuvaya ait olmalıdır.");
        }

        rule.setTournament(tournament);
        rule.setTargetMatch(targetMatch);
        rule.setTargetSide(targetSide);
        rule.setSourceType(sourceType);
        rule.setSourceValue(requireText(sourceValue, "Kaynak değeri").toUpperCase());
        return bracketRuleRepository.save(rule);
    }

    @Transactional
    public void deleteTournament(Long tournamentId) {
        Tournament tournament = getTournament(tournamentId);
        if (teamRepository.countByTournamentId(tournament.getId()) > 0
                || matchRepository.countByTournamentId(tournament.getId()) > 0
                || predictionRepository.countByMatchTournamentId(tournament.getId()) > 0
                || bracketRuleRepository.countByTournamentId(tournament.getId()) > 0) {
            throw new IllegalStateException("Bu turnuva bağlı takım, maç, tahmin veya eşleşme kuralı olduğu için silinemez.");
        }
        tournamentRepository.delete(tournament);
    }

    @Transactional
    public Long deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı."));
        Long tournamentId = team.getTournament().getId();
        if (matchRepository.countByHomeTeamIdOrAwayTeamId(team.getId(), team.getId()) > 0) {
            throw new IllegalStateException("Bu takım maçlarda kullanıldığı için silinemez.");
        }
        teamRepository.delete(team);
        return tournamentId;
    }

    @Transactional
    public Long deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Maç bulunamadı."));
        Long tournamentId = match.getTournament().getId();
        if (predictionRepository.countByMatchId(match.getId()) > 0) {
            throw new IllegalStateException("Bu maç için tahmin olduğu için silinemez.");
        }
        if (bracketRuleRepository.countByTargetMatchId(match.getId()) > 0) {
            throw new IllegalStateException("Bu maç eşleşme kurallarında kullanıldığı için silinemez.");
        }
        matchRepository.delete(match);
        return tournamentId;
    }

    @Transactional
    public Long deleteBracketRule(Long ruleId) {
        BracketRule rule = bracketRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Eşleşme kuralı bulunamadı."));
        Long tournamentId = rule.getTournament().getId();
        bracketRuleRepository.delete(rule);
        return tournamentId;
    }

    private Tournament getTournament(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("Turnuva zorunludur.");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnuva bulunamadı."));
    }

    private Team getOptionalTeam(Long teamId, Long tournamentId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı."));
        if (!team.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("Takım seçili turnuvaya ait olmalıdır.");
        }
        return team;
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " zorunludur.");
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
