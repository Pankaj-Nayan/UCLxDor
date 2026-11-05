package com.Bd.or.Ballondor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {}

    public record PlayerView(Long id, Integer externalPlayerId, String name, Integer age, String nationality,
                             String position, String currentClub, String photoUrl) {}

    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record StatisticsView(PlayerView player, Integer season, Integer appearances, Integer goals,
                                 Integer assists, Integer minutesPlayed, BigDecimal averageRating,
                                 Integer shots, Integer shotsOnTarget, Integer keyPasses, Integer passAccuracy,
                                 Integer tackles, Integer interceptions, Integer saves, Integer goalsConceded) {}

    public record PredictionView(PlayerView player, Integer season, double performanceScore,
                                 double estimatedWinningProbability, String probabilityScope,
                                 Map<String, Double> factorPercentiles, Map<String, Double> factorWeights,
                                 Map<String, Double> scoreContributions,
                                 List<String> limitations) {}

    public record LeaderboardView(Integer season, String competition, String probabilityScope,
                                  List<PredictionView> contenders) {}

    public record CompareRequest(@NotNull @Min(2000) @Max(2100) Integer season,
                                 @NotEmpty @Size(min = 2, max = 10) List<@NotNull @Positive Integer> playerIds) {}

    public record ComparisonPlayer(Integer externalPlayerId, String name, Integer goals, Integer assists,
                                   Integer appearances, Integer minutesPlayed, BigDecimal averageRating,
                                   double performanceScore, double estimatedWinningProbability) {}

    public record ComparisonView(Integer season, List<ComparisonPlayer> players) {}

    public record SeasonView(Integer season, String competition, Integer competitionId) {}

    public record RefreshView(Integer season, int playersRefreshed, String message) {}
}
