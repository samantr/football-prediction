package com.example.footballprediction.controller;

import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.service.PredictionScoreRow;
import com.example.footballprediction.service.PredictionService;
import com.example.footballprediction.service.ScoringService;
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

@Controller
public class MyPredictionsController {

    private final TournamentService tournamentService;
    private final UserService userService;
    private final ScoringService scoringService;
    private final PredictionService predictionService;

    public MyPredictionsController(
            TournamentService tournamentService,
            UserService userService,
            ScoringService scoringService,
            PredictionService predictionService
    ) {
        this.tournamentService = tournamentService;
        this.userService = userService;
        this.scoringService = scoringService;
        this.predictionService = predictionService;
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
        List<PredictionScoreRow> readOnlyRows = selectedTournamentId == null
                ? List.of()
                : scoringService.adminReport(selectedTournamentId);

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("rows", rows);
        model.addAttribute("readOnlyRows", readOnlyRows);
        model.addAttribute("totalScore", scoringService.totalScore(rows));
        return "my-predictions";
    }

    @PostMapping("/my-predictions/{predictionId}/delete")
    public String deletePrediction(
            @PathVariable Long predictionId,
            @RequestParam(required = false) Long tournamentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.getByEmail(authentication.getName());
            predictionService.deletePrediction(user.getId(), predictionId, isAdmin(authentication));
            redirectAttributes.addFlashAttribute("success", "Tahmin silindi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToMyPredictions(tournamentId);
    }

    private String redirectToMyPredictions(Long tournamentId) {
        return tournamentId == null ? "redirect:/my-predictions" : "redirect:/my-predictions?tournamentId=" + tournamentId;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
