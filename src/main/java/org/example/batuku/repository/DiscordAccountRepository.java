package org.example.batuku.repository;

import org.example.batuku.domain.DiscordAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DiscordAccountRepository extends JpaRepository<DiscordAccount, Long> {
    Optional<DiscordAccount> findByUserId(Long userId);
    Optional<DiscordAccount> findByDiscordUserId(String discordUserId);
}
