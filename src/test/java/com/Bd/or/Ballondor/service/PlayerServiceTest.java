package com.Bd.or.Ballondor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Bd.or.Ballondor.entity.Player;
import com.Bd.or.Ballondor.external.ExternalPlayer;
import com.Bd.or.Ballondor.external.FootballApiClient;
import com.Bd.or.Ballondor.external.PlayerSeasonData;
import com.Bd.or.Ballondor.mapper.EntityMapper;
import com.Bd.or.Ballondor.repository.PlayerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PlayerServiceTest {
    @Test
    void searchesProviderAndKeepsSameNamePlayersDistinctByProviderId() {
        PlayerRepository repository = org.mockito.Mockito.mock(PlayerRepository.class);
        FootballApiClient api = org.mockito.Mockito.mock(FootballApiClient.class);
        PlayerService service = new PlayerService(repository, api, new EntityMapper());
        Player first = new Player(101, "Alex Silva");
        Player second = new Player(202, "Alex Silva");
        when(api.searchPlayers("Alex Silva")).thenReturn(List.of(
                new PlayerSeasonData(new ExternalPlayer(101, "Alex Silva", 21, null, null, null, null, null), null),
                new PlayerSeasonData(new ExternalPlayer(202, "Alex Silva", 28, null, null, null, null, null), null)));
        when(repository.findByExternalPlayerId(101)).thenReturn(Optional.empty());
        when(repository.findByExternalPlayerId(202)).thenReturn(Optional.empty());
        when(repository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByNameContainingIgnoreCase("Alex Silva", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        var result = service.search("Alex Silva", PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
        assertEquals(101, result.getContent().get(0).externalPlayerId());
        assertEquals(202, result.getContent().get(1).externalPlayerId());
        verify(api).searchPlayers("Alex Silva");
    }
}
