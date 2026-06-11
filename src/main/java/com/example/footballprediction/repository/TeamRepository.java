package com.example.footballprediction.repository;

import com.example.footballprediction.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByTournamentIdOrderByGroupCodeAscNameAsc(Long tournamentId);

    long countByTournamentId(Long tournamentId);

    Optional<Team> findByTournamentIdAndCode(Long tournamentId, String code);

    Optional<Team> findByTournamentIdAndExternalProviderAndExternalId(Long tournamentId, String externalProvider, String externalId);
}
