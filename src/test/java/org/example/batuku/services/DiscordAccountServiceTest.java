package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.DiscordAccount;
import org.example.batuku.domain.User;
import org.example.batuku.repository.DiscordAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordAccountServiceTest {

    @Mock DiscordAccountRepository discordAccountRepository;
    @InjectMocks DiscordAccountService service;

    private DiscordAccount linkedAccount(User user) {
        DiscordAccount acc = new DiscordAccount();
        acc.setUser(user);
        acc.setDiscordUserId("discord123");
        acc.setDiscordUsername("testUser#0001");
        return acc;
    }

    @Test
    @DisplayName("getAccount devolve mapa com dados quando conta está ligada")
    void getAccountLigada() {
        User fan = TestData.user(1L, User.UserRole.FAN);
        when(discordAccountRepository.findByUserId(1L)).thenReturn(Optional.of(linkedAccount(fan)));

        Map<String, Object> result = service.getAccount(fan);

        assertEquals("discord123",      result.get("discordUserId"));
        assertEquals("testUser#0001",   result.get("discordUsername"));
        assertNotNull(result.get("linkedAt"));
    }

    @Test
    @DisplayName("getAccount devolve linked=false quando conta não está ligada")
    void getAccountNaoLigada() {
        User fan = TestData.user(1L, User.UserRole.FAN);
        when(discordAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.getAccount(fan);

        assertEquals(false, result.get("linked"));
    }

    @Test
    @DisplayName("unlinkAccount desliga e devolve unlinked quando conta existe")
    void unlinkAccountExiste() {
        User fan = TestData.user(1L, User.UserRole.FAN);
        DiscordAccount acc = linkedAccount(fan);
        when(discordAccountRepository.findByUserId(1L)).thenReturn(Optional.of(acc));

        String result = service.unlinkAccount(fan);

        assertEquals("unlinked", result);
        verify(discordAccountRepository).delete(acc);
    }

    @Test
    @DisplayName("unlinkAccount devolve not_linked quando conta não existe")
    void unlinkAccountNaoExiste() {
        User fan = TestData.user(1L, User.UserRole.FAN);
        when(discordAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        String result = service.unlinkAccount(fan);

        assertEquals("not_linked", result);
        verify(discordAccountRepository, never()).delete(any());
    }

    @Test
    @DisplayName("generateLinkToken devolve token não nulo e não vazio")
    void generateLinkToken() {
        User fan = TestData.user(1L, User.UserRole.FAN);

        String token = service.generateLinkToken(fan);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }
}
