package com.Bd.or.Ballondor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "seasons", uniqueConstraints = @UniqueConstraint(name = "uk_season_competition_year", columnNames = {"competition_id", "season_year"}),
        indexes = @Index(name = "idx_season_year", columnList = "season_year"))
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "season_year", nullable = false)
    private Integer seasonYear;
    @Column(name = "competition_name", nullable = false, length = 100)
    private String competitionName;
    @Column(name = "competition_id", nullable = false)
    private Integer competitionId;
    @Column(name = "candidate_pool_complete", nullable = false)
    private boolean candidatePoolComplete;

    protected Season() {}

    public Season(Integer seasonYear, String competitionName, Integer competitionId) {
        this.seasonYear = seasonYear;
        this.competitionName = competitionName;
        this.competitionId = competitionId;
    }

    public Long getId() { return id; }
    public Integer getSeasonYear() { return seasonYear; }
    public String getCompetitionName() { return competitionName; }
    public Integer getCompetitionId() { return competitionId; }
    public boolean isCandidatePoolComplete() { return candidatePoolComplete; }
    public void markCandidatePoolComplete() { this.candidatePoolComplete = true; }
    public void markCandidatePoolIncomplete() { this.candidatePoolComplete = false; }
}
