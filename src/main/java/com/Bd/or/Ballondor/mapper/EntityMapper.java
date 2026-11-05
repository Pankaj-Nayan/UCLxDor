package com.Bd.or.Ballondor.mapper;

import com.Bd.or.Ballondor.dto.ApiDtos;
import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.entity.PlayerStatistics;
import com.Bd.or.Ballondor.external.ExternalPlayer;
import com.Bd.or.Ballondor.external.ExternalStatistics;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {
    public Player updatePlayer(Player player, ExternalPlayer external) {
        player.updateProfile(external.name(), external.age(), external.nationality(), external.position(), external.clubName(),
                external.clubId(), external.photoUrl());
        return player;
    }

    public PlayerStatistics updateStatistics(PlayerStatistics target, ExternalStatistics source) {
        target.update(source.appearances(), source.goals(), source.assists(), source.minutes(), source.rating(),
                source.shots(), source.shotsOnTarget(), source.keyPasses(), source.passAccuracy(),
                source.tackles(), source.interceptions(), source.saves(), source.goalsConceded());
        return target;
    }

    public ApiDtos.PlayerView toPlayerView(Player player) {
        return new ApiDtos.PlayerView(player.getId(), player.getExternalPlayerId(), player.getName(),
                player.getAge(), player.getNationality(), player.getPosition(), player.getCurrentClub(), player.getPhotoUrl());
    }

    public ApiDtos.StatisticsView toStatisticsView(PlayerStatistics stats) {
        return new ApiDtos.StatisticsView(toPlayerView(stats.getPlayer()), stats.getSeason().getSeasonYear(),
                stats.getAppearances(), stats.getGoals(), stats.getAssists(), stats.getMinutesPlayed(),
                stats.getAverageRating(), stats.getShots(), stats.getShotsOnTarget(), stats.getKeyPasses(),
                stats.getPassAccuracy(), stats.getTackles(), stats.getInterceptions(), stats.getSaves(), stats.getGoalsConceded());
    }
}
