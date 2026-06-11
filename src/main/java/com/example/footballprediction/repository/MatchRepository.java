package com.example.footballprediction.repository;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByKickoffAtAscMatchNoAsc(Long tournamentId);

    List<Match> findByTournamentIdAndStageOrderByMatchNoAsc(Long tournamentId, MatchStage stage);

    List<Match> findByTournamentIdAndStageAndGroupCodeOrderByMatchNoAsc(Long tournamentId, MatchStage stage, String groupCode);

    Optional<Match> findByTournamentIdAndMatchNo(Long tournamentId, Integer matchNo);

    Optional<Match> findByTournamentIdAndExternalProviderAndExternalId(Long tournamentId, String externalProvider, String externalId);

    long countByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);

    @Query("select coalesce(max(m.matchNo), 0) from Match m where m.tournament.id = :tournamentId")
    int findMaxMatchNoByTournamentId(@Param("tournamentId") Long tournamentId);

    long countByTournamentId(Long tournamentId);
}
