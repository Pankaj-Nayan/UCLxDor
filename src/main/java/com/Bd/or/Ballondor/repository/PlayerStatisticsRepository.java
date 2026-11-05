package com.Bd.or.Ballondor.repository;

import com.Bd.or.Ballondor.entity.PlayerStatistics;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
    Optional<PlayerStatistics> findByPlayer_ExternalPlayerIdAndSeason_CompetitionIdAndSeason_SeasonYear(
            Integer externalPlayerId, Integer competitionId, Integer seasonYear);
    List<PlayerStatistics> findAllBySeason_CompetitionIdAndSeason_SeasonYear(Integer competitionId, Integer seasonYear);
}
