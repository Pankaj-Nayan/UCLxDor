package com.Bd.or.Ballondor.service;

import com.Bd.or.Ballondor.config.FootballApiProperties;
import com.Bd.or.Ballondor.dto.ApiDtos;
import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import com.Bd.or.Ballondor.entity.Season;
import com.Bd.or.Ballondor.exception.ApiException;
import com.Bd.or.Ballondor.external.FootballApiClient;
import com.Bd.or.Ballondor.external.PlayerSeasonData;
import com.Bd.or.Ballondor.mapper.EntityMapper;
import com.Bd.or.Ballondor.repository.PlayerStatisticsRepository;
import com.Bd.or.Ballondor.repository.SeasonRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
    private static final String COMPETITION_NAME = "UEFA Champions League";
    private final FootballApiClient footballApiClient;
    private final FootballApiProperties properties;
    private final PlayerService playerService;
    private final PlayerStatisticsRepository statisticsRepository;
    private final SeasonRepository seasonRepository;
    private final EntityMapper mapper;

    public StatisticsService(FootballApiClient footballApiClient, FootballApiProperties properties,
                             PlayerService playerService, PlayerStatisticsRepository statisticsRepository,
                             SeasonRepository seasonRepository, EntityMapper mapper) {
        this.footballApiClient = footballApiClient;
        this.properties = properties;
        this.playerService = playerService;
        this.statisticsRepository = statisticsRepository;
        this.seasonRepository = seasonRepository;
        this.mapper = mapper;
    }

    @Transactional
    public List<ApiDtos.SeasonView> supportedSeasons() {
        return footballApiClient.getSupportedSeasons().stream()
                .map(this::getOrCreateSeason)
                .map(season -> new ApiDtos.SeasonView(season.getSeasonYear(), season.getCompetitionName(), season.getCompetitionId()))
                .toList();
    }

    @Transactional
    public ApiDtos.StatisticsView getPlayerStatistics(int externalPlayerId, int seasonYear) {
        Season season = supportedSeason(seasonYear);
        PlayerSeasonData data = footballApiClient.getPlayerStatistics(externalPlayerId, seasonYear);
        Player player = playerService.savePlayer(data);
        PlayerStatistics statistics = saveStatistics(player, season, data);
        return mapper.toStatisticsView(statistics);
    }

    @Transactional
    public List<PlayerStatistics> getCompleteSeasonCandidates(int seasonYear) {
        Season season = supportedSeason(seasonYear);
        if (!season.isCandidatePoolComplete()) refreshSeason(seasonYear);
        return statisticsRepository.findAllBySeason_CompetitionIdAndSeason_SeasonYear(
                properties.getCompetitionId(), seasonYear);
    }

    @Transactional
    public ApiDtos.RefreshView refreshSeason(int seasonYear) {
        Season season = supportedSeason(seasonYear);
        List<PlayerSeasonData> candidates = footballApiClient.getSeasonPlayers(seasonYear);
        int refreshed = 0;
        for (PlayerSeasonData data : candidates) {
            if (data.statistics() == null) continue;
            Player player = playerService.savePlayer(data);
            saveStatistics(player, season, data);
            refreshed++;
        }
        if (refreshed == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND", "No usable player statistics exist for this season");
        }
        season.markCandidatePoolComplete();
        seasonRepository.save(season);
        return new ApiDtos.RefreshView(seasonYear, refreshed, "Season player statistics refreshed from the provider");
    }

    private Season supportedSeason(int seasonYear) {
        Season existing = seasonRepository.findByCompetitionIdAndSeasonYear(properties.getCompetitionId(), seasonYear).orElse(null);
        if (existing != null) return existing;
        List<Integer> years = footballApiClient.getSupportedSeasons();
        if (!years.contains(seasonYear)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SEASON", "The provider does not report player-statistics coverage for this UCL season");
        }
        return getOrCreateSeason(seasonYear);
    }

    private Season getOrCreateSeason(int seasonYear) {
        return seasonRepository.findByCompetitionIdAndSeasonYear(properties.getCompetitionId(), seasonYear)
                .orElseGet(() -> seasonRepository.save(new Season(seasonYear, COMPETITION_NAME, properties.getCompetitionId())));
    }

    private PlayerStatistics saveStatistics(Player player, Season season, PlayerSeasonData data) {
        if (data.statistics() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STATISTICS_NOT_FOUND", "The provider returned no player statistics for the selected season");
        }
        PlayerStatistics stats = statisticsRepository
                .findByPlayer_ExternalPlayerIdAndSeason_CompetitionIdAndSeason_SeasonYear(
                        player.getExternalPlayerId(), properties.getCompetitionId(), season.getSeasonYear())
                .orElseGet(() -> new PlayerStatistics(player, season));
        mapper.updateStatistics(stats, data.statistics());
        return statisticsRepository.save(stats);
    }
}
