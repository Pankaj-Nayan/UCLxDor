package com.Bd.or.Ballondor.prediction;

import com.Bd.or.Ballondor.config.PredictionProperties;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PredictionEngine {
    private final StatisticsProcessor processor;
    private final PredictionProperties properties;

    public PredictionEngine(StatisticsProcessor processor, PredictionProperties properties) {
        this.processor = processor;
        this.properties = properties;
    }

    public List<ScoredPlayer> score(List<PlayerStatistics> statistics) {
        List<PlayerStatistics> eligible = new ArrayList<>();
        List<Map<String, Double>> metrics = new ArrayList<>();
        for (PlayerStatistics stats : statistics) {
            if (stats.getAppearances() == null || stats.getAppearances() <= 0
                    || stats.getMinutesPlayed() == null || stats.getMinutesPlayed() <= 0) continue;
            Map<String, Double> playerMetrics = metrics(stats);
            if (weights(stats).keySet().stream().noneMatch(playerMetrics::containsKey)) continue;
            eligible.add(stats);
            metrics.add(playerMetrics);
        }
        if (eligible.isEmpty()) return List.of();
        List<ScoredPlayer> scores = new ArrayList<>();
        for (int i = 0; i < eligible.size(); i++) {
            Map<String, Double> weights = weights(eligible.get(i));
            Map<String, Double> percentiles = new LinkedHashMap<>();
            Map<String, Double> normalizedWeights = new LinkedHashMap<>();
            Map<String, Double> contributions = new LinkedHashMap<>();
            double weightedScore = 0;
            double availableWeight = 0;
            for (Map.Entry<String, Double> weight : weights.entrySet()) {
                Double value = metrics.get(i).get(weight.getKey());
                if (value == null) continue;
                double percentile = percentile(eligible, metrics, i, weight.getKey());
                percentiles.put(weight.getKey(), round(percentile * 100));
                double effectiveWeight = weight.getValue();
                availableWeight += effectiveWeight;
                weightedScore += percentile * effectiveWeight;
            }
            if (availableWeight > 0) {
                for (String factor : percentiles.keySet()) {
                    normalizedWeights.put(factor, round(weights.get(factor) / availableWeight * 100));
                    contributions.put(factor, round(percentiles.get(factor) * weights.get(factor) / availableWeight));
                }
            }
            scores.add(new ScoredPlayer(eligible.get(i), availableWeight == 0 ? 0 : round(weightedScore / availableWeight * 100),
                    Map.copyOf(percentiles), Map.copyOf(normalizedWeights), Map.copyOf(contributions)));
        }
        scores.sort(Comparator.comparingDouble(ScoredPlayer::performanceScore).reversed());
        return List.copyOf(scores);
    }

    public List<Double> probabilities(List<ScoredPlayer> scores) {
        if (scores.isEmpty()) return List.of();
        double temperature = properties.getSoftmaxTemperature();
        if (!Double.isFinite(temperature) || temperature <= 0) {
            throw new IllegalStateException("PREDICTION_SOFTMAX_TEMPERATURE must be a positive finite number");
        }
        double maximum = scores.stream().mapToDouble(ScoredPlayer::performanceScore).max().orElse(0);
        double[] exp = scores.stream().mapToDouble(s -> Math.exp((s.performanceScore() - maximum) / temperature)).toArray();
        double sum = 0;
        for (double value : exp) sum += value;
        List<Double> result = new ArrayList<>();
        for (double value : exp) result.add(value / sum * 100.0);
        return List.copyOf(result);
    }

    private Map<String, Double> metrics(PlayerStatistics stats) {
        ProcessedStatistics processed = processor.process(stats);
        Map<String, Double> values = new LinkedHashMap<>();
        put(values, "goalContributionsPer90", processed.goalContributionsPer90());
        put(values, "shotsOnTargetPer90", processed.shotsOnTargetPer90());
        put(values, "keyPassesPer90", processed.keyPassesPer90());
        put(values, "defensiveActionsPer90", processed.defensiveActionsPer90());
        put(values, "savesPer90", processed.savesPer90());
        put(values, "goalsConcededPer90", processed.goalsConcededPer90());
        if (stats.getAverageRating() != null) values.put("averageRating", stats.getAverageRating().doubleValue());
        if (stats.getPassAccuracy() != null) values.put("passAccuracy", stats.getPassAccuracy().doubleValue());
        return values;
    }

    private void put(Map<String, Double> values, String name, Double value) {
        if (value != null) values.put(name, value);
    }

    private Map<String, Double> weights(PlayerStatistics stats) {
        String position = stats.getPlayer().getPosition();
        if (position != null && position.toLowerCase().contains("goal")) {
            return Map.of("averageRating", .30, "savesPer90", .30, "goalsConcededPer90", .20,
                    "passAccuracy", .10, "goalContributionsPer90", .10);
        }
        if (position != null && position.toLowerCase().contains("def")) {
            return Map.of("averageRating", .25, "defensiveActionsPer90", .35, "goalContributionsPer90", .10,
                    "keyPassesPer90", .10, "passAccuracy", .10, "shotsOnTargetPer90", .10);
        }
        if (position != null && position.toLowerCase().contains("mid")) {
            return Map.of("averageRating", .25, "goalContributionsPer90", .20, "keyPassesPer90", .20,
                    "defensiveActionsPer90", .15, "shotsOnTargetPer90", .10, "passAccuracy", .10);
        }
        return Map.of("averageRating", .25, "goalContributionsPer90", .35, "shotsOnTargetPer90", .15,
                "keyPassesPer90", .10, "defensiveActionsPer90", .10, "passAccuracy", .05);
    }

    private double percentile(List<PlayerStatistics> cohort, List<Map<String, Double>> metrics, int index, String feature) {
        String role = role(cohort.get(index));
        long peersInRole = 0;
        for (int i = 0; i < cohort.size(); i++) {
            if (role(cohort.get(i)).equals(role) && metrics.get(i).get(feature) != null) peersInRole++;
        }
        boolean compareByRole = peersInRole > 1;
        long less = 0;
        long equal = 0;
        long count = 0;
        double current = metrics.get(index).get(feature);
        for (int i = 0; i < cohort.size(); i++) {
            if (compareByRole && !role(cohort.get(i)).equals(role)) continue;
            Double candidate = metrics.get(i).get(feature);
            if (candidate == null) continue;
            count++;
            if (feature.equals("goalsConcededPer90")) {
                if (candidate > current) less++;
                else if (Double.compare(candidate, current) == 0) equal++;
            } else {
                if (candidate < current) less++;
                else if (Double.compare(candidate, current) == 0) equal++;
            }
        }
        return count == 0 ? .5 : (less + equal * .5) / count;
    }

    private String role(PlayerStatistics stats) {
        String position = stats.getPlayer().getPosition();
        if (position == null) return "other";
        String normalized = position.toLowerCase();
        if (normalized.contains("goal")) return "goalkeeper";
        if (normalized.contains("def")) return "defender";
        if (normalized.contains("mid")) return "midfielder";
        if (normalized.contains("att") || normalized.contains("forward")) return "attacker";
        return "other";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
