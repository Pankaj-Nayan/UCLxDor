package com.Bd.or.Ballondor.service;

import com.Bd.or.Ballondor.config.FootballApiProperties;
import com.Bd.or.Ballondor.dto.ApiDtos;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import com.Bd.or.Ballondor.entity.Prediction;
import com.Bd.or.Ballondor.exception.ApiException;
import com.Bd.or.Ballondor.mapper.EntityMapper;
import com.Bd.or.Ballondor.prediction.PredictionEngine;
import com.Bd.or.Ballondor.prediction.ScoredPlayer;
import com.Bd.or.Ballondor.repository.PredictionRepository;
import com.Bd.or.Ballondor.repository.SeasonRepository;
import com.Bd.or.Ballondor.repository.PlayerStatisticsRepository;
import com.Bd.or.Ballondor.entity.Season;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PredictionService {
    private static final String PROBABILITY_SCOPE = "Model-estimated relative share within the provider-reported UEFA Champions League player-statistics cohort; not actual Ballon d'Or odds.";
    private static final List<String> LIMITATIONS = List.of(
            "This is a UCL-statistics-only model, not a complete Ballon d'Or prediction.",
            "Team trophies and knockout-stage contributions are not included because the player-statistics endpoint does not provide a reliable stage/team-achievement measure.",
            "Match-to-match consistency cannot be measured from season aggregates; average rating is used as an overall-performance measure, not a consistency measure.",
            "A player's probability is relative to all eligible players returned by the provider for this season; it is not an official or calibrated award probability.");

    private final StatisticsService statisticsService;
    private final PredictionEngine engine;
    private final PredictionRepository predictionRepository;
    private final SeasonRepository seasonRepository;
    private final PlayerStatisticsRepository statisticsRepository;
    private final EntityMapper mapper;
    private final FootballApiProperties properties;
    private final ObjectMapper objectMapper;

    public PredictionService(StatisticsService statisticsService, PredictionEngine engine,
                             PredictionRepository predictionRepository, SeasonRepository seasonRepository,
                             PlayerStatisticsRepository statisticsRepository, EntityMapper mapper,
                             FootballApiProperties properties, ObjectMapper objectMapper) {
        this.statisticsService = statisticsService;
        this.engine = engine;
        this.predictionRepository = predictionRepository;
        this.seasonRepository = seasonRepository;
        this.statisticsRepository = statisticsRepository;
        this.mapper = mapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApiDtos.PredictionView getPlayerPrediction(int externalPlayerId, int seasonYear) {
        List<ApiDtos.PredictionView> leaderboard = calculateLeaderboard(seasonYear);
        return leaderboard.stream().filter(item -> item.player().externalPlayerId() == externalPlayerId).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND",
                        "Player is not eligible for a prediction because the provider has no usable season statistics"));
    }

    @Transactional
    public ApiDtos.LeaderboardView getLeaderboard(int seasonYear) {
        return new ApiDtos.LeaderboardView(seasonYear, "UEFA Champions League", PROBABILITY_SCOPE,
                calculateLeaderboard(seasonYear));
    }

    @Transactional
    public ApiDtos.ComparisonView compare(ApiDtos.CompareRequest request) {
        if (request.playerIds().stream().distinct().count() != request.playerIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_PLAYER", "Each player ID may appear only once in a comparison");
        }
        List<ApiDtos.PredictionView> leaderboard = calculateLeaderboard(request.season());
        Map<Integer, ApiDtos.PredictionView> byId = new HashMap<>();
        leaderboard.forEach(view -> byId.put(view.player().externalPlayerId(), view));
        List<PlayerStatistics> statistics = statisticsRepository.findAllBySeason_CompetitionIdAndSeason_SeasonYear(
                properties.getCompetitionId(), request.season());
        Map<Integer, PlayerStatistics> statsById = new HashMap<>();
        statistics.forEach(stats -> statsById.put(stats.getPlayer().getExternalPlayerId(), stats));

        List<ApiDtos.ComparisonPlayer> comparisons = new ArrayList<>();
        for (Integer id : request.playerIds()) {
            ApiDtos.PredictionView prediction = byId.get(id);
            PlayerStatistics stats = statsById.get(id);
            if (prediction == null || stats == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND",
                        "One or more requested players have no usable statistics for this season");
            }
            comparisons.add(new ApiDtos.ComparisonPlayer(id, stats.getPlayer().getName(), stats.getGoals(),
                    stats.getAssists(), stats.getAppearances(), stats.getMinutesPlayed(), stats.getAverageRating(),
                    prediction.performanceScore(), prediction.estimatedWinningProbability()));
        }
        return new ApiDtos.ComparisonView(request.season(), comparisons);
    }

    private List<ApiDtos.PredictionView> calculateLeaderboard(int seasonYear) {
        List<PlayerStatistics> cohort = statisticsService.getCompleteSeasonCandidates(seasonYear);
        List<ScoredPlayer> scores = engine.score(cohort);
        if (scores.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND", "No players have enough available statistics for this season");
        }
        List<Double> probabilities = engine.probabilities(scores);
        Season season = seasonRepository.findByCompetitionIdAndSeasonYear(properties.getCompetitionId(), seasonYear)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SEASON", "The selected UCL season is unavailable"));

        List<Prediction> saved = new ArrayList<>();
        List<ApiDtos.PredictionView> views = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            ScoredPlayer scored = scores.get(i);
            double probability = probabilities.get(i);
            views.add(new ApiDtos.PredictionView(mapper.toPlayerView(scored.statistics().getPlayer()), seasonYear,
                    scored.performanceScore(), probability, PROBABILITY_SCOPE, scored.factorPercentiles(),
                    scored.factorWeights(), scored.scoreContributions(), LIMITATIONS));
            saved.add(new Prediction(scored.statistics().getPlayer(), season, scored.performanceScore(),
                    probability, Instant.now(), serializeBreakdown(scored)));
        }
        predictionRepository.saveAll(saved);
        return List.copyOf(views);
    }

    private String serializeBreakdown(ScoredPlayer scored) {
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("factorPercentiles", scored.factorPercentiles());
        breakdown.put("factorWeights", scored.factorWeights());
        breakdown.put("scoreContributions", scored.scoreContributions());
        breakdown.put("limitations", LIMITATIONS);
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize prediction explanation", exception);
        }
    }
}
