package com.Bd.or.Ballondor.prediction;

import com.Bd.or.Ballondor.entity.PlayerStatistics;
import java.util.Map;

public record ScoredPlayer(PlayerStatistics statistics, double performanceScore,
                           Map<String, Double> factorPercentiles, Map<String, Double> factorWeights,
                           Map<String, Double> scoreContributions) {}
