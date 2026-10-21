package org.example.batuku.services;

import org.example.batuku.controllers.DiscordLinkTokenStore;
import org.example.batuku.domain.User;
import org.example.batuku.repository.DiscordAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DiscordAccountService {

    private final DiscordAccountRepository discordAccountRepository;

    public DiscordAccountService(DiscordAccountRepository discordAccountRepository) {
        this.discordAccountRepository = discordAccountRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAccount(User user) {
        return discordAccountRepository.findByUserId(user.getId())
                .map(acc -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("discordUserId",   acc.getDiscordUserId());
                    m.put("discordUsername", acc.getDiscordUsername());
                    m.put("linkedAt",        acc.getLinkedAt().toString());
                    return m;
                })
                .orElseGet(() -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("linked", false);
                    return m;
                });
    }

    @Transactional
    public String unlinkAccount(User user) {
        return discordAccountRepository.findByUserId(user.getId())
                .map(acc -> { discordAccountRepository.delete(acc); return "unlinked"; })
                .orElse("not_linked");
    }

    public String generateLinkToken(User user) {
        return DiscordLinkTokenStore.generate(user.getId());
    }
}
