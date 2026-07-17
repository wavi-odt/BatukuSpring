package org.example.batuku.repository;

import org.example.batuku.domain.DiscordEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscordEventRepository extends JpaRepository<DiscordEvent, Long> {
    List<DiscordEvent> findByDiscordAccountIdOrderByCreatedAtDesc(Long discordAccountId);
}
