package com.Bd.or.Ballondor.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import com.Bd.or.Ballondor.entity.Season;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StatisticsProcessorTest {
    private final StatisticsProcessor processor = new StatisticsProcessor();

    @Test
    void convertsTotalsToPerNinetyRatesAndPreservesMissingFields() {
        PlayerStatistics statistics = stats(1800, 20, 10, 4, 8);

        ProcessedStatistics result = processor.process(statistics);

        assertEquals(1.5, result.goalContributionsPer90(), 0.0001);
        assertEquals(0.5, result.shotsOnTargetPer90(), 0.0001);
        assertEquals(2.0, result.keyPassesPer90(), 0.0001);
        assertEquals(0.6, result.defensiveActionsPer90(), 0.0001);
        assertNull(result.savesPer90());
    }

    @Test
    void zeroMinutesMakesRatesUnavailableInsteadOfInventingZero() {
        ProcessedStatistics result = processor.process(stats(0, 0, 0, null, null));
        assertNull(result.goalContributionsPer90());
        assertNull(result.keyPassesPer90());
    }

    static PlayerStatistics stats(int minutes, int goals, int assists, Integer keyPasses, Integer tackles) {
        Player player = new Player(10, "Sample Player");
        player.updateProfile("Sample Player", 25, "Country", "Midfielder", "Club", 1, null);
        PlayerStatistics stats = new PlayerStatistics(player, new Season(2024, "UEFA Champions League", 2));
        stats.update(20, goals, assists, minutes, new BigDecimal("7.50"), 30, 10, keyPasses,
                80, tackles, tackles == null ? null : 4, null, null);
        return stats;
    }
}
