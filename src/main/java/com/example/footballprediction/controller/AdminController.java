package com.example.footballprediction.controller;

import com.example.footballprediction.domain.BracketRule;
import com.example.footballprediction.domain.BracketSourceType;
import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.TargetSide;
import com.example.footballprediction.domain.Team;
import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.repository.BracketRuleRepository;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.TeamRepository;
import com.example.footballprediction.repository.UserRepository;
import com.example.footballprediction.service.AdminDataService;
import com.example.footballprediction.service.BracketGenerationResult;
import com.example.footballprediction.service.BracketService;
import com.example.footballprediction.service.PredictionScoreRow;
import com.example.footballprediction.service.ScoringService;
import com.example.footballprediction.service.TournamentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final DateTimeFormatter DATETIME_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final TournamentService tournamentService;
    private final AdminDataService adminDataService;
    private final ScoringService scoringService;
    private final BracketService bracketService;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final BracketRuleRepository bracketRuleRepository;

    public AdminController(
            TournamentService tournamentService,
            AdminDataService adminDataService,
            ScoringService scoringService,
            BracketService bracketService,
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            UserRepository userRepository,
            BracketRuleRepository bracketRuleRepository
    ) {
        this.tournamentService = tournamentService;
        this.adminDataService = adminDataService;
        this.scoringService = scoringService;
        this.bracketService = bracketService;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.bracketRuleRepository = bracketRuleRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        Tournament selectedTournament = tournamentService.findDefaultTournament();
        model.addAttribute("tournamentCount", tournamentService.findAll().size());
        model.addAttribute("teamCount", selectedTournament == null ? 0 : teamRepository.findByTournamentIdOrderByGroupCodeAscNameAsc(selectedTournament.getId()).size());
        model.addAttribute("matchCount", selectedTournament == null ? 0 : matchRepository.countByTournamentId(selectedTournament.getId()));
        model.addAttribute("userCount", userRepository.findByRoleOrderByDisplayNameAsc(Role.USER).size());
        model.addAttribute("selectedTournament", selectedTournament);
        return "admin/index";
    }

    @GetMapping("/tournaments")
    public String tournaments(@RequestParam(required = false) Long editId, Model model) {
        Tournament editTournament = editId == null
                ? new Tournament()
                : tournamentService.findAll().stream()
                .filter(tournament -> tournament.getId().equals(editId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found."));

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("editTournament", editTournament);
        return "admin/tournaments";
    }

    @PostMapping("/tournaments")
    public String saveTournament(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam String season,
            @RequestParam(defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            tournamentService.saveTournament(id, name, season, active);
            redirectAttributes.addFlashAttribute("success", "Tournament saved.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/tournaments";
    }

    @GetMapping("/teams")
    public String teams(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long editId,
            Model model
    ) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();
        List<Team> teams = selectedTournamentId == null
                ? List.of()
                : teamRepository.findByTournamentIdOrderByGroupCodeAscNameAsc(selectedTournamentId);
        Team editTeam = editId == null
                ? new Team()
                : teamRepository.findById(editId).orElseThrow(() -> new IllegalArgumentException("Team not found."));

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("teams", teams);
        model.addAttribute("editTeam", editTeam);
        return "admin/teams";
    }

    @PostMapping("/teams")
    public String saveTeam(
            @RequestParam(required = false) Long id,
            @RequestParam Long tournamentId,
            @RequestParam String name,
            @RequestParam String code,
            @RequestParam(required = false) String groupCode,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminDataService.saveTeam(id, tournamentId, name, code, groupCode);
            redirectAttributes.addFlashAttribute("success", "Team saved.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToAdmin("teams", tournamentId);
    }

    @GetMapping("/matches")
    public String matches(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long editId,
            Model model
    ) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();
        List<Team> teams = selectedTournamentId == null
                ? List.of()
                : teamRepository.findByTournamentIdOrderByGroupCodeAscNameAsc(selectedTournamentId);
        List<Match> matches = selectedTournamentId == null
                ? List.of()
                : matchRepository.findByTournamentIdOrderByKickoffAtAscMatchNoAsc(selectedTournamentId);
        Match editMatch = editId == null
                ? new Match()
                : matchRepository.findById(editId).orElseThrow(() -> new IllegalArgumentException("Match not found."));

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("teams", teams);
        model.addAttribute("matches", matches);
        model.addAttribute("editMatch", editMatch);
        model.addAttribute("editKickoffAt", formatDateTime(editMatch.getKickoffAt()));
        model.addAttribute("stages", MatchStage.values());
        model.addAttribute("statuses", MatchStatus.values());
        return "admin/matches";
    }

    @PostMapping("/matches")
    public String saveMatch(
            @RequestParam(required = false) Long id,
            @RequestParam Long tournamentId,
            @RequestParam Integer matchNo,
            @RequestParam MatchStage stage,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) Long homeTeamId,
            @RequestParam(required = false) Long awayTeamId,
            @RequestParam(required = false) String placeholderHome,
            @RequestParam(required = false) String placeholderAway,
            @RequestParam String kickoffAt,
            @RequestParam MatchStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminDataService.saveMatch(
                    id,
                    tournamentId,
                    matchNo,
                    stage,
                    groupCode,
                    homeTeamId,
                    awayTeamId,
                    placeholderHome,
                    placeholderAway,
                    parseDateTime(kickoffAt),
                    status
            );
            redirectAttributes.addFlashAttribute("success", "Match saved.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToAdmin("matches", tournamentId);
    }

    @GetMapping("/results")
    public String results(@RequestParam(required = false) Long tournamentId, Model model) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();
        List<Match> matches = selectedTournamentId == null
                ? List.of()
                : matchRepository.findByTournamentIdOrderByKickoffAtAscMatchNoAsc(selectedTournamentId);

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("matches", matches);
        return "admin/results";
    }

    @PostMapping("/results/{matchId}")
    public String saveResult(
            @PathVariable Long matchId,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam Integer homeScore,
            @RequestParam Integer awayScore,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminDataService.enterResult(matchId, homeScore, awayScore);
            redirectAttributes.addFlashAttribute("success", "Result saved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToAdmin("results", tournamentId);
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) Long tournamentId, Model model) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();
        List<PredictionScoreRow> rows = scoringService.adminReport(selectedTournamentId);

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("leaderboard", scoringService.leaderboard(selectedTournamentId));
        model.addAttribute("rows", rows);
        model.addAttribute("users", userRepository.findByRoleOrderByDisplayNameAsc(Role.USER));
        model.addAttribute("matches", selectedTournamentId == null ? List.of() : matchRepository.findByTournamentIdOrderByKickoffAtAscMatchNoAsc(selectedTournamentId));
        return "admin/reports";
    }

    @GetMapping("/generate-next-round")
    public String generateNextRound(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long editRuleId,
            Model model
    ) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();
        List<Match> matches = selectedTournamentId == null
                ? List.of()
                : matchRepository.findByTournamentIdOrderByKickoffAtAscMatchNoAsc(selectedTournamentId);
        List<BracketRule> rules = selectedTournamentId == null
                ? List.of()
                : bracketRuleRepository.findByTournamentIdOrdered(selectedTournamentId);
        BracketRule editRule = editRuleId == null
                ? new BracketRule()
                : bracketRuleRepository.findById(editRuleId).orElseThrow(() -> new IllegalArgumentException("Bracket rule not found."));

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("matches", matches);
        model.addAttribute("rules", rules);
        model.addAttribute("editRule", editRule);
        model.addAttribute("targetSides", TargetSide.values());
        model.addAttribute("sourceTypes", BracketSourceType.values());
        return "admin/generate-next-round";
    }

    @PostMapping("/generate-next-round/rules")
    public String saveBracketRule(
            @RequestParam(required = false) Long id,
            @RequestParam Long tournamentId,
            @RequestParam Long targetMatchId,
            @RequestParam TargetSide targetSide,
            @RequestParam BracketSourceType sourceType,
            @RequestParam String sourceValue,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminDataService.saveBracketRule(id, tournamentId, targetMatchId, targetSide, sourceType, sourceValue);
            redirectAttributes.addFlashAttribute("success", "Bracket rule saved.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToAdmin("generate-next-round", tournamentId);
    }

    @PostMapping("/generate-next-round")
    public String runGenerateNextRound(
            @RequestParam Long tournamentId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            BracketGenerationResult result = bracketService.generateNextRound(tournamentId);
            redirectAttributes.addFlashAttribute("messages", result.getMessages());
            if (result.isAdminReviewRequired()) {
                redirectAttributes.addFlashAttribute("error", "Admin review required.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Bracket generation finished.");
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToAdmin("generate-next-round", tournamentId);
    }

    private String redirectToAdmin(String page, Long tournamentId) {
        return tournamentId == null
                ? "redirect:/admin/" + page
                : "redirect:/admin/" + page + "?tournamentId=" + tournamentId;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Kickoff time is required.");
        }
        return LocalDateTime.parse(value.trim());
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATETIME_LOCAL);
    }
}
