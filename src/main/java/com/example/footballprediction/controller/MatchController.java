package com.example.footballprediction.controller;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.service.PredictionService;
import com.example.footballprediction.service.TournamentService;
import com.example.footballprediction.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MatchController {

    private final TournamentService tournamentService;
    private final MatchRepository matchRepository;
    private final PredictionService predictionService;
    private final UserService userService;

    public MatchController(
            TournamentService tournamentService,
            MatchRepository matchRepository,
            PredictionService predictionService,
            UserService userService
    ) {
        this.tournamentService = tournamentService;
        this.matchRepository = matchRepository;
        this.predictionService = predictionService;
        this.userService = userService;
    }

    @GetMapping("/matches")
    public String matches(
            @RequestParam(required = false) Long tournamentId,
            Authentication authentication,
            Model model
    ) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        User user = userService.getByEmail(authentication.getName());
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();

        List<Match> matches = selectedTournamentId == null
                ? List.of()
                : matchRepository.findByTournamentIdOrderByKickoffAtAscMatchNoAsc(selectedTournamentId);
        Map<Long, Prediction> predictionsByMatchId = selectedTournamentId == null
                ? Map.of()
                : predictionService.findPredictionMapForUser(user.getId(), selectedTournamentId);
        Map<Long, Boolean> editableByMatchId = matches.stream()
                .collect(Collectors.toMap(Match::getId, predictionService::canEditPrediction));

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("matches", matches);
        model.addAttribute("predictionsByMatchId", predictionsByMatchId);
        model.addAttribute("editableByMatchId", editableByMatchId);
        return "matches";
    }

    @PostMapping("/matches/{matchId}/predictions")
    public String savePrediction(
            @PathVariable Long matchId,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam Integer predictedHomeScore,
            @RequestParam Integer predictedAwayScore,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.getByEmail(authentication.getName());
            predictionService.savePrediction(user.getId(), matchId, predictedHomeScore, predictedAwayScore);
            redirectAttributes.addFlashAttribute("success", "Prediction saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToMatches(tournamentId);
    }

    private String redirectToMatches(Long tournamentId) {
        return tournamentId == null ? "redirect:/matches" : "redirect:/matches?tournamentId=" + tournamentId;
    }
}
