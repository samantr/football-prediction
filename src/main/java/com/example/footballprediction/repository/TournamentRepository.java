package com.example.footballprediction.repository;

import com.example.footballprediction.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findAllByOrderByActiveDescNameAsc();

    Optional<Tournament> findFirstByActiveTrueOrderByNameAsc();
}
