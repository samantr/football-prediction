package com.example.footballprediction.repository;

import com.example.footballprediction.domain.BracketRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BracketRuleRepository extends JpaRepository<BracketRule, Long> {

    @Query("""
            select r
            from BracketRule r
            join r.targetMatch m
            where r.tournament.id = :tournamentId
            order by m.matchNo asc, r.targetSide asc
            """)
    List<BracketRule> findByTournamentIdOrdered(@Param("tournamentId") Long tournamentId);
}
