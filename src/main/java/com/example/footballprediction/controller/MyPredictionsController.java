package com.example.footballprediction.controller;

import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.service.PredictionScoreRow;
import com.example.footballprediction.service.ScoringService;
import com.example.footballprediction.service.TournamentService;
import com.example.footballprediction.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MyPredictionsController {

    private final TournamentService tournamentService;
    private final UserService userService;
    private final ScoringService scoringService;

    public MyPredictionsController(
            TournamentService tournamentService,
            UserService userService,
            ScoringService scoringService
    ) {
        this.tournamentService = tournamentService;
        this.userService = userService;
        this.scoringService = scoringService;
    }

    @GetMapping("/my-predictions")
    public String myPredictions(
            @RequestParam(required = false) Long tournamentId,
            Authentication authentication,
            Model model
    ) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        User user = userService.getByEmail(authentication.getName());
        Long selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();
        List<PredictionScoreRow> rows = scoringService.userReport(user.getId(), selectedTournamentId);

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("rows", rows);
        model.addAttribute("totalScore", scoringService.totalScore(rows));
        return "my-predictions";
    }
}
