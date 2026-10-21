package org.example.batuku.services;

import org.example.batuku.domain.DiscordAccount;
import org.example.batuku.repository.DiscordAccountRepository;
import org.example.batuku.repository.DiscordEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordEventServiceTest {

    @Mock DiscordAccountRepository discordAccountRepository;
    @Mock DiscordEventRepository   discordEventRepository;
    @InjectMocks DiscordEventService service;

    @Test
    @DisplayName("processEvent lança IllegalArgumentException para eventType inválido")
    void processEventTipoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processEvent("discord123", "INVALID_TYPE", null, null));
        verify(discordEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("processEvent devolve ignored quando conta Discord não está ligada")
    void processEventContaNaoLigada() {
        when(discordAccountRepository.findByDiscordUserId("discord123")).thenReturn(Optional.empty());

        Map<String, String> result = service.processEvent("discord123", "MESSAGE", null, null);

        assertEquals("ignored", result.get("status"));
        verify(discordEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("processEvent guarda evento e devolve saved quando conta existe")
    void processEventValido() {
        DiscordAccount acc = new DiscordAccount();
        acc.setDiscordUserId("discord123");
        acc.setDiscordUsername("testUser#0001");
        when(discordAccountRepository.findByDiscordUserId("discord123")).thenReturn(Optional.of(acc));

        Map<String, String> result = service.processEvent("discord123", "MESSAGE", "ref1", "{}");

        assertEquals("saved", result.get("status"));
        verify(discordEventRepository).save(any());
    }
}
