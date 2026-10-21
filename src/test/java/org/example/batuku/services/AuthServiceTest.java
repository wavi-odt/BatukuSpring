package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Location;
import org.example.batuku.domain.User;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository                 userRepository;
    @Mock RoleRepository                 roleRepository;
    @Mock PasswordEncoder                passwordEncoder;
    @Mock PendingRegistrationRepository  pendingRepository;
    @Mock PendingArtistClaimRepository   pendingArtistClaimRepository;
    @Mock LocationRepository             locationRepository;
    @Mock GamificationService            gamificationService;
    @Mock EmailService                   emailService;
    @Mock ArtistProfileRepository        artistProfileRepository;
    @Mock RefreshTokenService            refreshTokenService;
    @InjectMocks AuthService service;

    @Test
    @DisplayName("getLocations devolve lista com value e group")
    void getLocations() {
        Location l = new Location();
        l.setValue("Lisboa");
        l.setLocationGroup("Portugal");
        when(locationRepository.findAll()).thenReturn(List.of(l));

        List<Map<String, String>> result = service.getLocations();

        assertEquals(1, result.size());
        assertEquals("Lisboa",   result.get(0).get("value"));
        assertEquals("Portugal", result.get(0).get("group"));
    }

    @Test
    @DisplayName("getMe devolve UserResponse sem perfil artista quando não existe")
    void getMeSemPerfil() {
        User fan = TestData.user(1L, User.UserRole.FAN);
        when(userRepository.findByEmail("user1@batuku.test")).thenReturn(Optional.of(fan));
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        UserResponse r = service.getMe("user1@batuku.test");

        assertNotNull(r);
        assertNull(r.getArtistProfileId());
    }

    @Test
    @DisplayName("getMe devolve UserResponse com artistProfileId quando existe perfil")
    void getMeComPerfil() {
        User artist = TestData.user(2L, User.UserRole.ARTIST);
        ArtistProfile ap = TestData.artistProfile(10L, artist);
        when(userRepository.findByEmail("user2@batuku.test")).thenReturn(Optional.of(artist));
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(ap));

        UserResponse r = service.getMe("user2@batuku.test");

        assertEquals(10L, r.getArtistProfileId());
    }

    @Test
    @DisplayName("logout invalida token e revoga refresh tokens quando utilizador existe")
    void logoutUtilizadorExiste() {
        User fan = TestData.user(1L, User.UserRole.FAN);
        when(userRepository.findByEmail("user1@batuku.test")).thenReturn(Optional.of(fan));
        when(userRepository.save(fan)).thenReturn(fan);

        service.logout("user1@batuku.test");

        assertNotNull(fan.getTokenValidFrom());
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    @DisplayName("logout não faz nada quando utilizador não existe")
    void logoutUtilizadorNaoExiste() {
        when(userRepository.findByEmail("ghost@batuku.test")).thenReturn(Optional.empty());

        service.logout("ghost@batuku.test");

        verify(refreshTokenService, never()).revokeAllForUser(any());
    }
}
