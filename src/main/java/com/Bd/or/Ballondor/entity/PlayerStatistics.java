package com.Bd.or.Ballondor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "player_statistics",
        uniqueConstraints = @UniqueConstraint(name = "uk_player_statistics_season", columnNames = {"player_id", "season_id"}),
        indexes = @Index(name = "idx_statistics_season", columnList = "season_id"))
public class PlayerStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    @Column(name = "minutes_played")
    private Integer minutesPlayed;
    @Column(precision = 5, scale = 2)
    private BigDecimal averageRating;
    private Integer shots;
    @Column(name = "shots_on_target")
    private Integer shotsOnTarget;
    @Column(name = "key_passes")
    private Integer keyPasses;
    @Column(name = "pass_accuracy")
    private Integer passAccuracy;
    private Integer tackles;
    private Integer interceptions;
    private Integer saves;
    @Column(name = "goals_conceded")
    private Integer goalsConceded;

    protected PlayerStatistics() {}

    public PlayerStatistics(Player player, Season season) {
        this.player = player;
        this.season = season;
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public Season getSeason() { return season; }
    public Integer getAppearances() { return appearances; }
    public Integer getGoals() { return goals; }
    public Integer getAssists() { return assists; }
    public Integer getMinutesPlayed() { return minutesPlayed; }
    public BigDecimal getAverageRating() { return averageRating; }
    public Integer getShots() { return shots; }
    public Integer getShotsOnTarget() { return shotsOnTarget; }
    public Integer getKeyPasses() { return keyPasses; }
    public Integer getPassAccuracy() { return passAccuracy; }
    public Integer getTackles() { return tackles; }
    public Integer getInterceptions() { return interceptions; }
    public Integer getSaves() { return saves; }
    public Integer getGoalsConceded() { return goalsConceded; }

    public void update(Integer appearances, Integer goals, Integer assists, Integer minutesPlayed, BigDecimal averageRating,
                       Integer shots, Integer shotsOnTarget, Integer keyPasses, Integer passAccuracy,
                       Integer tackles, Integer interceptions, Integer saves, Integer goalsConceded) {
        this.appearances = appearances;
        this.goals = goals;
        this.assists = assists;
        this.minutesPlayed = minutesPlayed;
        this.averageRating = averageRating;
        this.shots = shots;
        this.shotsOnTarget = shotsOnTarget;
        this.keyPasses = keyPasses;
        this.passAccuracy = passAccuracy;
        this.tackles = tackles;
        this.interceptions = interceptions;
        this.saves = saves;
        this.goalsConceded = goalsConceded;
    }
}
