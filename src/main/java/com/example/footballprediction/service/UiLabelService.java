package com.example.footballprediction.service;

import com.example.footballprediction.domain.BracketSourceType;
import com.example.footballprediction.domain.MatchStage;
import com.example.footballprediction.domain.MatchStatus;
import com.example.footballprediction.domain.TargetSide;
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
}
