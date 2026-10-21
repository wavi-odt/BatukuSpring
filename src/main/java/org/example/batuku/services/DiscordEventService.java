package org.example.batuku.services;

import org.example.batuku.domain.DiscordAccount;
import org.example.batuku.domain.DiscordEvent;
import org.example.batuku.repository.DiscordAccountRepository;
import org.example.batuku.repository.DiscordEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DiscordEventService {

    private final DiscordAccountRepository discordAccountRepository;
    private final DiscordEventRepository   discordEventRepository;

    public DiscordEventService(DiscordAccountRepository discordAccountRepository,
                               DiscordEventRepository discordEventRepository) {
        this.discordAccountRepository = discordAccountRepository;
        this.discordEventRepository   = discordEventRepository;
    }

    @Transactional
    public Map<String, String> processEvent(String discordUserId, String eventType,
                                            String externalReference, String metadataJson) {
        DiscordEvent.EventType type;
        try {
            type = DiscordEvent.EventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown eventType: " + eventType);
        }

        DiscordAccount account = discordAccountRepository.findByDiscordUserId(discordUserId).orElse(null);
        if (account == null) {
            return Map.of("status", "ignored", "reason", "Discord account not linked");
        }

        DiscordEvent event = new DiscordEvent();
        event.setDiscordAccount(account);
        event.setEventType(type);
        event.setExternalReference(externalReference);
        event.setMetadataJson(metadataJson);
        discordEventRepository.save(event);

        return Map.of("status", "saved");
    }
}
