package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.User;
import org.example.batuku.dto.UserDetailResponse;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.*;
import org.example.batuku.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock UserRepository          userRepository;
    @Mock FollowRepository        followRepository;
    @Mock ArtistFollowRepository  artistFollowRepository;
    @Mock ArtistProfileRepository artistProfileRepository;
    @Mock LocationRepository      locationRepository;
    @Mock PlayRepository          playRepository;
    @Mock UserPointsRepository    userPointsRepository;
    @Mock UserBadgeRepository     userBadgeRepository;
    @Mock BadgeRepository         badgeRepository;
    @Mock PasswordEncoder         passwordEncoder;
    @Mock FileStorageService      storageService;
    @InjectMocks UserProfileService service;

    private User fan;
    private User artist;

    @BeforeEach
    void setUp() {
        fan    = TestData.user(1L, User.UserRole.FAN);
        artist = TestData.user(2L, User.UserRole.ARTIST);
    }

    @Test
    @DisplayName("getUserDetail devolve perfil com followers corretos para não-artista")
    void getUserDetailFan() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(fan));
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(followRepository.countByFolloweeId(1L)).thenReturn(5L);
        when(followRepository.countByFollowerId(1L)).thenReturn(3L);
        when(artistFollowRepository.countByFollowerId(1L)).thenReturn(2L);
        when(userPointsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userBadgeRepository.findByUserId(1L)).thenReturn(List.of());
        when(badgeRepository.findAll()).thenReturn(List.of());

        UserDetailResponse r = service.getUserDetail(1L, null);

        assertEquals(5L, r.followers());
        assertEquals(5L, r.following());
        assertNull(r.artistProfileId());
        assertFalse(r.isFollowing());
    }

    @Test
    @DisplayName("updateNameUsername lança conflito quando username já existe")
    void updateNameUsernameConflito() {
        fan.setUsername("oldname");
        when(userRepository.existsByUsername("newname")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.updateNameUsername(fan, "Nome", "newname"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateNameUsername actualiza sem conflito quando username é o próprio")
    void updateNameUsernameProprioUsername() {
        fan.setUsername("sameuser");
        when(userRepository.save(fan)).thenReturn(fan);

        UserResponse r = service.updateNameUsername(fan, "Novo Nome", "sameuser");

        assertNotNull(r);
        verify(userRepository).save(fan);
    }

    @Test
    @DisplayName("updateLocation lança erro com localização inválida")
    void updateLocationInvalida() {
        when(locationRepository.existsByValue("Atlantis")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateLocation(fan, "Atlantis"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLocation aceita localização vazia (sem localização)")
    void updateLocationVazia() {
        when(userRepository.save(fan)).thenReturn(fan);

        UserResponse r = service.updateLocation(fan, "");

        assertNotNull(r);
        assertNull(fan.getLocation());
    }

    @Test
    @DisplayName("setPassword lança conflito quando utilizador já tem password")
    void setPasswordJaTemPassword() {
        fan.setPassword("encoded_pass");

        assertThrows(IllegalStateException.class,
                () -> service.setPassword(fan, "newpassword"));
    }

    @Test
    @DisplayName("setPassword lança erro quando password tem menos de 8 caracteres")
    void setPasswordCurta() {
        fan.setPassword("{OAUTH}google");

        assertThrows(IllegalArgumentException.class,
                () -> service.setPassword(fan, "short"));
    }

    @Test
    @DisplayName("changePassword lança erro quando password atual está errada")
    void changePasswordErrada() {
        fan.setPassword("encoded");
        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(fan, "wrongpass", "newpassword123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("setMarketplaceRole define papel e devolve o valor")
    void setMarketplaceRoleValido() {
        when(userRepository.save(fan)).thenReturn(fan);

        String result = service.setMarketplaceRole(fan, "fan");

        assertEquals("FAN", result);
    }

    @Test
    @DisplayName("setMarketplaceRole lança conflito quando papel já está definido")
    void setMarketplaceRoleJaDefinido() {
        fan.setMarketplaceRole("FAN");

        assertThrows(IllegalStateException.class,
                () -> service.setMarketplaceRole(fan, "PRODUCER"));
    }

    @Test
    @DisplayName("setMarketplaceRole lança erro para papel inválido")
    void setMarketplaceRoleInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setMarketplaceRole(fan, "ADMIN"));
    }

    @Test
    @DisplayName("getRecentlyPlayed lança NoSuchElementException para utilizador inexistente")
    void getRecentlyPlayedNaoExiste() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> service.getRecentlyPlayed(99L));
    }

    @Test
    @DisplayName("listUsers devolve todos quando q é nulo")
    void listUsersSeQ() {
        when(userRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(fan));

        var result = service.listUsers(null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("toggleVerify inverte o estado verified e devolve UserResponse")
    void toggleVerify() {
        fan.setVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(fan));
        when(userRepository.save(fan)).thenReturn(fan);

        var r = service.toggleVerify(1L);

        assertNotNull(r);
        assertTrue(fan.isVerified());
    }

    @Test
    @DisplayName("getTopGenres devolve lista com percentagem calculada")
    void getTopGenres() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(fan));
        Object[] row1 = new Object[]{"Batuku", 80L};
        Object[] row2 = new Object[]{"Morna",  20L};
        when(playRepository.findTopGenresByUser(1L)).thenReturn(List.of(row1, row2));

        List<Map<String, Object>> result = service.getTopGenres(1L);

        assertEquals(2, result.size());
        assertEquals(80, (int) result.get(0).get("pct"));
        assertEquals(20, (int) result.get(1).get("pct"));
    }
}
