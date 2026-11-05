package com.Bd.or.Ballondor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.Bd.or.Ballondor.config.FootballApiProperties;
import com.Bd.or.Ballondor.config.PredictionProperties;
import com.Bd.or.Ballondor.dto.ApiDtos;
import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import com.Bd.or.Ballondor.entity.Season;
import com.Bd.or.Ballondor.mapper.EntityMapper;
import com.Bd.or.Ballondor.prediction.PredictionEngine;
import com.Bd.or.Ballondor.prediction.StatisticsProcessor;
import com.Bd.or.Ballondor.repository.PlayerStatisticsRepository;
import com.Bd.or.Ballondor.repository.PredictionRepository;
import com.Bd.or.Ballondor.repository.SeasonRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class PredictionServiceTest {
    @Test
    void comparesPlayersFromTheSameSeasonAndReturnsTheirCalculatedValues() {
        StatisticsService statisticsService = mock(StatisticsService.class);
        PredictionRepository predictionRepository = mock(PredictionRepository.class);
        SeasonRepository seasonRepository = mock(SeasonRepository.class);
        PlayerStatisticsRepository statisticsRepository = mock(PlayerStatisticsRepository.class);
        FootballApiProperties apiProperties = new FootballApiProperties();
        apiProperties.setCompetitionId(2);
        PlayerStatistics a = stats(1, "Player A", 5);
        PlayerStatistics b = stats(2, "Player B", 15);
        when(statisticsService.getCompleteSeasonCandidates(2024)).thenReturn(List.of(a, b));
        when(seasonRepository.findByCompetitionIdAndSeasonYear(2, 2024))
                .thenReturn(Optional.of(new Season(2024, "UEFA Champions League", 2)));
        when(statisticsRepository.findAllBySeason_CompetitionIdAndSeason_SeasonYear(2, 2024)).thenReturn(List.of(a, b));

        PredictionService service = new PredictionService(statisticsService,
                new PredictionEngine(new StatisticsProcessor(), new PredictionProperties()),
                predictionRepository, seasonRepository, statisticsRepository, new EntityMapper(),
                apiProperties, new ObjectMapper());

        ApiDtos.ComparisonView result = service.compare(new ApiDtos.CompareRequest(2024, List.of(1, 2)));

        assertEquals(2024, result.season());
        assertEquals(5, result.players().get(0).goals());
        assertEquals(15, result.players().get(1).goals());
        assertEquals(2, result.players().size());
        assertEquals(100.0, result.players().stream().mapToDouble(ApiDtos.ComparisonPlayer::estimatedWinningProbability)
                .sum(), 0.000001);
    }

    private PlayerStatistics stats(int id, String name, int goals) {
        Player player = new Player(id, name);
        player.updateProfile(name, 24, "Country", "Forward", "Club", 1, null);
        PlayerStatistics statistics = new PlayerStatistics(player, new Season(2024, "UEFA Champions League", 2));
        statistics.update(8, goals, 2, 720, new BigDecimal("7.5"), 20, 10, 4, 80, 2, 2, null, null);
        return statistics;
    }
}
