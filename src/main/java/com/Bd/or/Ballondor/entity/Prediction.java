package com.Bd.or.Ballondor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "predictions")
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(name = "performance_score", nullable = false)
    private double performanceScore;
    @Column(name = "estimated_probability", nullable = false)
    private double estimatedProbability;
    @Column(name = "prediction_date", nullable = false)
    private Instant predictionDate;
    @Column(name = "scoring_breakdown", nullable = false, length = 2000)
    private String scoringBreakdown;

    protected Prediction() {}

    public Prediction(Player player, Season season, double performanceScore, double estimatedProbability,
                      Instant predictionDate, String scoringBreakdown) {
        this.player = player;
        this.season = season;
        this.performanceScore = performanceScore;
        this.estimatedProbability = estimatedProbability;
        this.predictionDate = predictionDate;
        this.scoringBreakdown = scoringBreakdown;
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public Season getSeason() { return season; }
    public double getPerformanceScore() { return performanceScore; }
    public double getEstimatedProbability() { return estimatedProbability; }
    public Instant getPredictionDate() { return predictionDate; }
    public String getScoringBreakdown() { return scoringBreakdown; }
}
