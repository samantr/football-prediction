package com.example.footballprediction.service;

import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.BracketRuleRepository;
import com.example.footballprediction.repository.MatchRepository;
import com.example.footballprediction.repository.PredictionRepository;
import com.example.footballprediction.repository.TeamRepository;
import com.example.footballprediction.repository.TournamentRepository;
import com.example.footballprediction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCleanupService {

    private final PredictionRepository predictionRepository;
    private final BracketRuleRepository bracketRuleRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    public AdminCleanupService(
            PredictionRepository predictionRepository,
            BracketRuleRepository bracketRuleRepository,
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            TournamentRepository tournamentRepository,
            UserRepository userRepository
    ) {
        this.predictionRepository = predictionRepository;
        this.bracketRuleRepository = bracketRuleRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AdminCleanupResult cleanTestData() {
        long predictionsDeleted = predictionRepository.count();
        long bracketRulesDeleted = bracketRuleRepository.count();
        long matchesDeleted = matchRepository.count();
        long teamsDeleted = teamRepository.count();
        long tournamentsDeleted = tournamentRepository.count();
        long usersDeleted = userRepository.countByRole(Role.USER);

        predictionRepository.deleteAll();
        bracketRuleRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        tournamentRepository.deleteAll();

        List<User> users = userRepository.findByRoleOrderByDisplayNameAsc(Role.USER);
        userRepository.deleteAll(users);

        return new AdminCleanupResult(
                predictionsDeleted,
                bracketRulesDeleted,
                matchesDeleted,
                teamsDeleted,
                tournamentsDeleted,
                usersDeleted
        );
    }
}
