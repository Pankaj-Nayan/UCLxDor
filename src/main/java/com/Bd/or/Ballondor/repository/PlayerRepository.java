package com.Bd.or.Ballondor.repository;

import com.Bd.or.Ballondor.entity.Player;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByExternalPlayerId(Integer externalPlayerId);
    Page<Player> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
