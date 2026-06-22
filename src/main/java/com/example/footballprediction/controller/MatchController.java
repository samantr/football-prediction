package com.example.footballprediction.controller;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.Prediction;
import com.example.footballprediction.domain.TargetSide;
import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.service.PredictionResultClassification;
import com.example.footballprediction.service.PredictionService;
import com.example.footballprediction.service.ScoringService;
import com.example.footballprediction.service.TournamentService;
import com.example.footballprediction.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final ScoringService scoringService;
    private final UserService userService;

    public MatchController(
            TournamentService tournamentService,
            MatchRepository matchRepository,
            PredictionService predictionService,
            ScoringService scoringService,
            UserService userService
    ) {
        this.tournamentService = tournamentService;
        this.matchRepository = matchRepository;
        this.predictionService = predictionService;
        this.scoringService = scoringService;
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
        Map<Long, PredictionResultClassification> classificationByMatchId = predictionsByMatchId.values().stream()
                .collect(Collectors.toMap(prediction -> prediction.getMatch().getId(), scoringService::classify));

        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("matches", matches);
        model.addAttribute("predictionsByMatchId", predictionsByMatchId);
        model.addAttribute("editableByMatchId", editableByMatchId);
        model.addAttribute("classificationByMatchId", classificationByMatchId);
        return "matches";
    }

    @PostMapping("/matches/{matchId}/predictions")
    public String savePrediction(
            @PathVariable Long matchId,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String predictedHomeScore,
            @RequestParam(required = false) String predictedAwayScore,
            @RequestParam(required = false) String predictedPenaltyWinner,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.getByEmail(authentication.getName());
            predictionService.savePrediction(
                    user.getId(),
                    matchId,
                    parseScore(predictedHomeScore),
                    parseScore(predictedAwayScore),
                    parseTargetSide(predictedPenaltyWinner)
            );
            redirectAttributes.addFlashAttribute("success", "Tahmin kaydedildi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToMatches(tournamentId);
    }

    @PostMapping("/matches/{matchId}/predictions/ajax")
    public ResponseEntity<PredictionSaveResponse> savePredictionAjax(
            @PathVariable Long matchId,
            @RequestParam(required = false) String predictedHomeScore,
            @RequestParam(required = false) String predictedAwayScore,
            @RequestParam(required = false) String predictedPenaltyWinner,
            Authentication authentication
    ) {
        try {
            User user = userService.getByEmail(authentication.getName());
            Prediction prediction = predictionService.savePrediction(
                    user.getId(),
                    matchId,
                    parseScore(predictedHomeScore),
                    parseScore(predictedAwayScore),
                    parseTargetSide(predictedPenaltyWinner)
            );
            return ResponseEntity.ok(PredictionSaveResponse.success(
                    "Kaydedildi",
                    prediction.getPredictedHomeScore(),
                    prediction.getPredictedAwayScore(),
                    prediction.getPredictedPenaltyWinner()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(PredictionSaveResponse.failure(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(PredictionSaveResponse.failure(ex.getMessage()));
        }
    }

    private String redirectToMatches(Long tournamentId) {
        return tournamentId == null ? "redirect:/matches" : "redirect:/matches?tournamentId=" + tournamentId;
    }

    private Integer parseScore(String score) {
        if (score == null || score.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(score.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Tahmin skorları geçerli sayı olmalıdır.");
        }
    }

    private TargetSide parseTargetSide(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return TargetSide.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Penaltı galibi geçerli olmalıdır.");
        }
    }

    public record PredictionSaveResponse(
            boolean success,
            String message,
            Integer predictedHomeScore,
            Integer predictedAwayScore,
            TargetSide predictedPenaltyWinner
    ) {
        static PredictionSaveResponse success(
                String message,
                Integer predictedHomeScore,
                Integer predictedAwayScore,
                TargetSide predictedPenaltyWinner
        ) {
            return new PredictionSaveResponse(
                    true,
                    message,
                    predictedHomeScore,
                    predictedAwayScore,
                    predictedPenaltyWinner
            );
        }

        static PredictionSaveResponse failure(String message) {
            return new PredictionSaveResponse(false, message, null, null, null);
        }
    }
}
