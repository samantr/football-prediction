package com.example.footballprediction.repository;

import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByKickoffAtAscMatchNoAsc(Long tournamentId);

    List<Match> findByTournamentIdAndStageOrderByMatchNoAsc(Long tournamentId, MatchStage stage);

    List<Match> findByTournamentIdAndStageAndGroupCodeOrderByMatchNoAsc(Long tournamentId, MatchStage stage, String groupCode);

    Optional<Match> findByTournamentIdAndMatchNo(Long tournamentId, Integer matchNo);

    long countByTournamentId(Long tournamentId);
}
