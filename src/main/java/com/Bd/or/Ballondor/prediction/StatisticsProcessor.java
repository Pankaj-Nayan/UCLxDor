package com.Bd.or.Ballondor.prediction;

import com.Bd.or.Ballondor.entity.PlayerStatistics;
import org.springframework.stereotype.Component;

@Component
public class StatisticsProcessor {
    public ProcessedStatistics process(PlayerStatistics stats) {
        Integer minutes = stats.getMinutesPlayed();
        if (minutes == null || minutes <= 0) {
            return new ProcessedStatistics(null, null, null, null, null, null);
        }
        double ninetyMinutes = minutes / 90.0;
        return new ProcessedStatistics(rate(sum(stats.getGoals(), stats.getAssists()), ninetyMinutes),
                rate(stats.getShotsOnTarget(), ninetyMinutes), rate(stats.getKeyPasses(), ninetyMinutes),
                rate(sum(stats.getTackles(), stats.getInterceptions()), ninetyMinutes),
                rate(stats.getSaves(), ninetyMinutes), rate(stats.getGoalsConceded(), ninetyMinutes));
    }

    private Integer sum(Integer first, Integer second) {
        return first == null && second == null ? null : (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    private Double rate(Integer value, double nineties) {
        return value == null ? null : value / nineties;
    }
}
