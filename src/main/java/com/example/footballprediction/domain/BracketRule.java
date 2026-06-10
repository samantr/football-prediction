package com.example.footballprediction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bracket_rules")
public class BracketRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_match_id", nullable = false)
    private Match targetMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_side", nullable = false)
    private TargetSide targetSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private BracketSourceType sourceType;

    @Column(name = "source_value", nullable = false)
    private String sourceValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Match getTargetMatch() {
        return targetMatch;
    }

    public void setTargetMatch(Match targetMatch) {
        this.targetMatch = targetMatch;
    }

    public TargetSide getTargetSide() {
        return targetSide;
    }

    public void setTargetSide(TargetSide targetSide) {
        this.targetSide = targetSide;
    }

    public BracketSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(BracketSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }
}
