package com.example.footballprediction.repository;

import com.example.footballprediction.domain.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUserIdAndMatchId(Long userId, Long matchId);

    @Query("""
            select p
            from Prediction p
            join p.match m
            where p.user.id = :userId
            order by m.kickoffAt asc, m.matchNo asc
            """)
    List<Prediction> findByUserIdOrderByMatchKickoffAtAsc(@Param("userId") Long userId);

    @Query("""
            select p
            from Prediction p
            join p.match m
            where p.user.id = :userId and m.tournament.id = :tournamentId
            order by m.kickoffAt asc, m.matchNo asc
            """)
    List<Prediction> findByUserIdAndMatchTournamentIdOrderByMatchKickoffAtAsc(
            @Param("userId") Long userId,
            @Param("tournamentId") Long tournamentId
    );

    @Query("""
            select p
            from Prediction p
            join p.user u
            join p.match m
            where m.tournament.id = :tournamentId
            order by u.displayName asc, m.kickoffAt asc, m.matchNo asc
            """)
    List<Prediction> findByMatchTournamentIdOrderByUserDisplayNameAscMatchKickoffAtAsc(@Param("tournamentId") Long tournamentId);
}
