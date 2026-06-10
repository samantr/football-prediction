package com.example.footballprediction.controller;

import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.service.ScoringService;
import com.example.footballprediction.service.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LeaderboardController {

    private final TournamentService tournamentService;
    private final ScoringService scoringService;

    public LeaderboardController(TournamentService tournamentService, ScoringService scoringService) {
        this.tournamentService = tournamentService;
        this.scoringService = scoringService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/leaderboard";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(required = false) Long tournamentId, Model model) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("leaderboard", scoringService.leaderboard(selectedTournamentId));
        return "leaderboard";
    }
}
