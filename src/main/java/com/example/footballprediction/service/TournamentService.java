package com.example.footballprediction.service;

import com.example.footballprediction.domain.Tournament;
import com.example.footballprediction.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(readOnly = true)
    public List<Tournament> findAll() {
        return tournamentRepository.findAllByOrderByActiveDescNameAsc();
    }

    @Transactional(readOnly = true)
    public Tournament findDefaultTournament() {
        return tournamentRepository.findFirstByActiveTrueOrderByNameAsc()
                .or(() -> tournamentRepository.findAllByOrderByActiveDescNameAsc().stream().findFirst())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Tournament findSelectedTournament(Long tournamentId) {
        if (tournamentId == null) {
            return findDefaultTournament();
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnuva bulunamadı."));
    }

    @Transactional
    public Tournament saveTournament(Long id, String name, String season, boolean active) {
        Tournament tournament = id == null
                ? new Tournament()
                : tournamentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Turnuva bulunamadı."));

        tournament.setName(requireText(name, "Ad"));
        tournament.setSeason(requireText(season, "Sezon"));
        tournament.setActive(active);

        Tournament saved = tournamentRepository.save(tournament);

        if (active) {
            tournamentRepository.findAll().stream()
                    .filter(other -> !other.getId().equals(saved.getId()) && other.isActive())
                    .forEach(other -> other.setActive(false));
        }

        return saved;
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " zorunludur.");
        }
        return value.trim();
    }
}
