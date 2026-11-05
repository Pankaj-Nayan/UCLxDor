package com.Bd.or.Ballondor.controller;

import com.Bd.or.Ballondor.dto.ApiDtos;
import com.Bd.or.Ballondor.service.PlayerService;
import com.Bd.or.Ballondor.service.PredictionService;
import com.Bd.or.Ballondor.service.StatisticsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
public class PlayerController {
    private final PlayerService playerService;
    private final StatisticsService statisticsService;
    private final PredictionService predictionService;

    public PlayerController(PlayerService playerService, StatisticsService statisticsService,
                            PredictionService predictionService) {
        this.playerService = playerService;
        this.statisticsService = statisticsService;
        this.predictionService = predictionService;
    }

    @GetMapping("/players")
    public ApiDtos.PageView<ApiDtos.PlayerView> searchPlayers(
            @RequestParam("name") @NotBlank @Size(min = 3, max = 80) String name,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = playerService.search(name, PageRequest.of(page, size));
        return new ApiDtos.PageView<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/players/{externalPlayerId}")
    public ApiDtos.PlayerView getPlayer(@PathVariable("externalPlayerId") @Positive int externalPlayerId) {
        return playerService.getPlayer(externalPlayerId);
    }

    @GetMapping("/players/{externalPlayerId}/statistics")
    public ApiDtos.StatisticsView getPlayerStatistics(@PathVariable("externalPlayerId") @Positive int externalPlayerId,
                                                       @RequestParam("season") @Min(2000) @Max(2100) int season) {
        return statisticsService.getPlayerStatistics(externalPlayerId, season);
    }

    @GetMapping("/players/{externalPlayerId}/prediction")
    public ApiDtos.PredictionView getPlayerPrediction(@PathVariable("externalPlayerId") @Positive int externalPlayerId,
                                                       @RequestParam("season") @Min(2000) @Max(2100) int season) {
        return predictionService.getPlayerPrediction(externalPlayerId, season);
    }

    @GetMapping("/seasons/{season}/leaderboard")
    public ApiDtos.LeaderboardView getLeaderboard(@PathVariable("season") @Min(2000) @Max(2100) int season) {
        return predictionService.getLeaderboard(season);
    }

    @PostMapping("/players/compare")
    public ApiDtos.ComparisonView comparePlayers(@Valid @RequestBody ApiDtos.CompareRequest request) {
        return predictionService.compare(request);
    }

    @GetMapping("/seasons")
    public java.util.List<ApiDtos.SeasonView> getSupportedSeasons() {
        return statisticsService.supportedSeasons();
    }

    @PostMapping("/seasons/{season}/refresh")
    public ApiDtos.RefreshView refreshSeason(@PathVariable("season") @Min(2000) @Max(2100) int season) {
        return statisticsService.refreshSeason(season);
    }
}
