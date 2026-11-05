package com.Bd.or.Ballondor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "football.api")
@Validated
public class FootballApiProperties {
    @NotBlank
    private String baseUrl;
    private String key;
    @Positive
    private int competitionId = 2;
    @Min(2000)
    @Max(2100)
    private int season = 2024;
    @Positive
    private int maxPages = 100;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public int getCompetitionId() { return competitionId; }
    public void setCompetitionId(int competitionId) { this.competitionId = competitionId; }
    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }
}
