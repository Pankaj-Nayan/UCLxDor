package com.Bd.or.Ballondor.service;

import com.Bd.or.Ballondor.dto.ApiDtos;
import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.exception.ApiException;
import com.Bd.or.Ballondor.external.FootballApiClient;
import com.Bd.or.Ballondor.external.PlayerSeasonData;
import com.Bd.or.Ballondor.mapper.EntityMapper;
import com.Bd.or.Ballondor.repository.PlayerRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final FootballApiClient footballApiClient;
    private final EntityMapper mapper;

    public PlayerService(PlayerRepository playerRepository, FootballApiClient footballApiClient, EntityMapper mapper) {
        this.playerRepository = playerRepository;
        this.footballApiClient = footballApiClient;
        this.mapper = mapper;
    }

    @Transactional
    public Page<ApiDtos.PlayerView> search(String name, Pageable pageable) {
        List<PlayerSeasonData> matches = footballApiClient.searchPlayers(name.trim());
        matches.forEach(data -> savePlayer(data));
        return playerRepository.findByNameContainingIgnoreCase(name.trim(), pageable).map(mapper::toPlayerView);
    }

    @Transactional
    public ApiDtos.PlayerView getPlayer(int externalPlayerId) {
        return playerRepository.findByExternalPlayerId(externalPlayerId).map(mapper::toPlayerView)
                .orElseGet(() -> mapper.toPlayerView(savePlayer(footballApiClient.getPlayerDetails(externalPlayerId))));
    }

    @Transactional
    public Player savePlayer(PlayerSeasonData data) {
        if (data.player().name() == null || data.player().name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_FOOTBALL_API_RESPONSE", "Football data provider returned a player without a name");
        }
        Player player = playerRepository.findByExternalPlayerId(data.player().id())
                .orElseGet(() -> new Player(data.player().id(), data.player().name()));
        mapper.updatePlayer(player, data.player());
        return playerRepository.save(player);
    }
}
