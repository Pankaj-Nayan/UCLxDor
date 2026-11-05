package com.Bd.or.Ballondor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Bd.or.Ballondor.config.FootballApiProperties;
import com.Bd.or.Ballondor.exception.ApiException;
import com.Bd.or.Ballondor.external.FootballApiClient;
import com.Bd.or.Ballondor.mapper.EntityMapper;
import com.Bd.or.Ballondor.repository.PlayerStatisticsRepository;
import com.Bd.or.Ballondor.repository.SeasonRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {
    @Test
    void rejectsSeasonWithoutProviderPlayerCoverageBeforeFetchingPlayerStats() {
        FootballApiClient api = mock(FootballApiClient.class);
        SeasonRepository seasons = mock(SeasonRepository.class);
        FootballApiProperties properties = new FootballApiProperties();
        properties.setCompetitionId(2);
        when(seasons.findByCompetitionIdAndSeasonYear(2, 2023)).thenReturn(Optional.empty());
        when(api.getSupportedSeasons()).thenReturn(List.of(2024));
        StatisticsService service = new StatisticsService(api, properties, mock(PlayerService.class),
                mock(PlayerStatisticsRepository.class), seasons, new EntityMapper());

        ApiException error = assertThrows(ApiException.class, () -> service.getPlayerStatistics(55, 2023));

        assertEquals("INVALID_SEASON", error.getCode());
        verify(api, never()).getPlayerStatistics(55, 2023);
    }
}
