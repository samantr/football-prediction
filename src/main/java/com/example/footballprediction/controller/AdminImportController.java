package com.example.footballprediction.controller;

import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.service.FootballDataImportResult;
import com.example.footballprediction.service.FootballDataImportService;
import com.example.footballprediction.service.TournamentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/import")
public class AdminImportController {

    private static final String SYNC_TEAMS = "teams";
    private static final String SYNC_MATCHES = "matches";
    private static final String SYNC_ALL = "all";

    private final TournamentService tournamentService;
    private final FootballDataImportService footballDataImportService;

    public AdminImportController(
            TournamentService tournamentService,
            FootballDataImportService footballDataImportService
    ) {
        this.tournamentService = tournamentService;
        this.footballDataImportService = footballDataImportService;
    }

    @GetMapping
    public String importPage(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String competitionCode,
            @RequestParam(required = false) Integer season,
            Model model
    ) {
        Tournament selectedTournament = tournamentService.findSelectedTournament(tournamentId);
        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("selectedTournament", selectedTournament);
        model.addAttribute("competitionCode", normalizeCompetitionCode(competitionCode));
        model.addAttribute("season", normalizeSeason(season));
        model.addAttribute("tokenConfigured", footballDataImportService.isConfigured());
        return "admin/import";
    }

    @PostMapping
    public String sync(
            @RequestParam Long tournamentId,
            @RequestParam String competitionCode,
            @RequestParam Integer season,
            @RequestParam String syncType,
            RedirectAttributes redirectAttributes
    ) {
        String normalizedCompetitionCode = normalizeCompetitionCode(competitionCode);
        Integer normalizedSeason = normalizeSeason(season);

        if (!footballDataImportService.isConfigured()) {
            redirectAttributes.addFlashAttribute("error", "FOOTBALL_DATA_API_TOKEN tanımlı değil.");
            return redirectToImport(tournamentId, normalizedCompetitionCode, normalizedSeason);
        }

        try {
            FootballDataImportResult result = switch (syncType) {
                case SYNC_TEAMS -> footballDataImportService.syncTeams(tournamentId, normalizedCompetitionCode, normalizedSeason);
                case SYNC_MATCHES -> footballDataImportService.syncMatches(tournamentId, normalizedCompetitionCode, normalizedSeason);
                case SYNC_ALL -> footballDataImportService.syncAll(tournamentId, normalizedCompetitionCode, normalizedSeason);
                default -> throw new IllegalArgumentException("Geçersiz içe aktarım işlemi.");
            };
            redirectAttributes.addFlashAttribute("success", "İçe aktarım tamamlandı.");
            redirectAttributes.addFlashAttribute("messages", result.toMessages());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return redirectToImport(tournamentId, normalizedCompetitionCode, normalizedSeason);
    }

    private String redirectToImport(Long tournamentId, String competitionCode, Integer season) {
        String path = UriComponentsBuilder.fromPath("/admin/import")
                .queryParam("tournamentId", tournamentId)
                .queryParam("competitionCode", competitionCode)
                .queryParam("season", season)
                .toUriString();
        return "redirect:" + path;
    }

    private String normalizeCompetitionCode(String competitionCode) {
        String value = competitionCode == null ? "" : competitionCode.trim();
        return value.isEmpty() ? FootballDataImportService.DEFAULT_COMPETITION_CODE : value.toUpperCase();
    }

    private Integer normalizeSeason(Integer season) {
        return season == null ? FootballDataImportService.DEFAULT_SEASON : season;
    }
}
