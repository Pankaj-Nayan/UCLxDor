package com.Bd.or.Ballondor.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.Bd.or.Ballondor.config.PredictionProperties;
import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import com.Bd.or.Ballondor.entity.Season;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PredictionEngineTest {
    private final PredictionEngine engine = new PredictionEngine(new StatisticsProcessor(), new PredictionProperties());

    @Test
    void ranksPlayersFromTheWholeSeasonCohortAndNormalizesProbabilities() {
        PlayerStatistics lower = stats(11, "Player A", 5, 3, "Forward");
        PlayerStatistics higher = stats(12, "Player B", 20, 10, "Forward");

        List<ScoredPlayer> scores = engine.score(List.of(lower, higher));
        List<Double> probabilities = engine.probabilities(scores);

        assertEquals(2, scores.size());
        assertEquals(12, scores.get(0).statistics().getPlayer().getExternalPlayerId());
        assertTrue(scores.get(0).performanceScore() > scores.get(1).performanceScore());
        assertEquals(100.0, probabilities.stream().mapToDouble(Double::doubleValue).sum(), 0.000001);
        assertTrue(probabilities.get(0) > probabilities.get(1));
        assertEquals(scores.get(0).performanceScore(), scores.get(0).scoreContributions().values()
                .stream().mapToDouble(Double::doubleValue).sum(), 0.1);
        assertEquals(100.0, scores.get(0).factorWeights().values().stream()
                .mapToDouble(Double::doubleValue).sum(), 0.1);
    }

    @Test
    void excludesPlayersWithoutAnAppearanceAndHandlesAnEmptyCohort() {
        PlayerStatistics unused = stats(13, "Unused", 0, 0, "Midfielder");
        unused.update(0, 0, 0, 0, null, null, null, null, null, null, null, null, null);
        assertTrue(engine.score(List.of(unused)).isEmpty());
        PlayerStatistics unavailable = stats(14, "Unavailable", 0, 0, "Midfielder");
        unavailable.update(5, null, null, 450, null, null, null, null, null, null, null, null, null);
        assertTrue(engine.score(List.of(unavailable)).isEmpty());
        assertTrue(engine.probabilities(List.of()).isEmpty());
    }

    private PlayerStatistics stats(int id, String name, int goals, int assists, String position) {
        Player player = new Player(id, name);
        player.updateProfile(name, 25, "Country", position, "Club", 1, null);
        PlayerStatistics stats = new PlayerStatistics(player, new Season(2024, "UEFA Champions League", 2));
        stats.update(10, goals, assists, 900, new BigDecimal("7.50"), 30, 20, 12, 80, 5, 3, null, null);
        return stats;
    }
}
