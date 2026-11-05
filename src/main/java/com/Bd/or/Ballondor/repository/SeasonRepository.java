package com.Bd.or.Ballondor.repository;

import com.Bd.or.Ballondor.entity.Season;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByCompetitionIdAndSeasonYear(Integer competitionId, Integer seasonYear);
    List<Season> findAllByCompetitionIdOrderBySeasonYearDesc(Integer competitionId);
}
