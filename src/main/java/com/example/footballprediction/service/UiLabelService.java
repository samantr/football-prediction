package com.example.footballprediction.service;

import com.example.footballprediction.domain.BracketSourceType;
import com.example.footballprediction.domain.Match;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.TargetSide;
import com.example.footballprediction.domain.Team;
import org.springframework.stereotype.Component;

@Component("uiLabels")
public class UiLabelService {

    public String matchStage(MatchStage stage) {
        if (stage == null) {
            return "";
        }
        return switch (stage) {
            case GROUP -> "Grup";
            case ROUND_OF_16 -> "Son 16";
            case QUARTER_FINAL -> "Çeyrek final";
            case SEMI_FINAL -> "Yarı final";
            case THIRD_PLACE -> "Üçüncülük maçı";
            case FINAL -> "Final";
        };
    }

    public String matchStatus(MatchStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case SCHEDULED -> "Planlandı";
            case COMPLETED -> "Tamamlandı";
            case ADMIN_REVIEW_REQUIRED -> "Yönetici kontrolü gerekli";
        };
    }

    public String targetSide(TargetSide side) {
        if (side == null) {
            return "";
        }
        return switch (side) {
            case HOME -> "Ev sahibi";
            case AWAY -> "Deplasman";
        };
    }

    public boolean eliminationStage(MatchStage stage) {
        return stage != null && stage != MatchStage.GROUP;
    }

    public String matchSideLabel(Match match, TargetSide side) {
        if (match == null || side == null) {
            return "";
        }
        return side == TargetSide.HOME
                ? teamLabel(match.getHomeTeam(), match.getPlaceholderHome(), "Ev sahibi")
                : teamLabel(match.getAwayTeam(), match.getPlaceholderAway(), "Deplasman");
    }

    public String bracketSourceType(BracketSourceType sourceType) {
        if (sourceType == null) {
            return "";
        }
        return switch (sourceType) {
            case GROUP_WINNER -> "Grup birincisi";
            case GROUP_RUNNER_UP -> "Grup ikincisi";
            case MATCH_WINNER -> "Maç galibi";
            case MATCH_LOSER -> "Maç mağlubu";
        };
    }

    public String predictionResultClass(PredictionResultClassification classification) {
        if (classification == null) {
            return "prediction-pending";
        }
        return switch (classification) {
            case EXACT_SCORE -> "prediction-exact";
            case CORRECT_OUTCOME -> "prediction-outcome";
            case WRONG -> "prediction-wrong";
            case PENDING -> "prediction-pending";
        };
    }

    public String predictionResultLabel(PredictionResultClassification classification) {
        if (classification == null) {
            return "Bekliyor";
        }
        return switch (classification) {
            case EXACT_SCORE -> "Tam skor";
            case CORRECT_OUTCOME -> "Doğru sonuç";
            case WRONG -> "Yanlış";
            case PENDING -> "Bekliyor";
        };
    }

    private String teamLabel(Team team, String placeholder, String fallback) {
        if (team != null) {
            return team.getName();
        }
        if (placeholder != null && !placeholder.isBlank()) {
            return placeholder;
        }
        return fallback;
    }
}
