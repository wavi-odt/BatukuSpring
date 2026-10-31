package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.Playlist;
import org.example.batuku.domain.User;
import org.example.batuku.repository.*;
import org.example.batuku.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock PlaylistRepository        playlistRepository;
    @Mock PlaylistTrackRepository   playlistTrackRepository;
    @Mock TrackRepository           trackRepository;
    @Mock LikeRepository            likeRepository;
    @Mock SavedPlaylistRepository   savedPlaylistRepository;
    @Mock FileStorageService        storageService;
    @InjectMocks PlaylistService service;

    private User dono;
    private User outro;
    private Playlist playlistPrivada;

    @BeforeEach
    void setUp() {
        dono  = TestData.user(1L, User.UserRole.FAN);
        outro = TestData.user(2L, User.UserRole.FAN);

        playlistPrivada = new Playlist();
        playlistPrivada.setTitle("Privada");
        playlistPrivada.setUser(dono);
        ReflectionTestUtils.setField(playlistPrivada, "id", 10L);
    }

    @Test
    @DisplayName("O dono vê a sua própria playlist privada")
    void donoVePlaylistPrivada() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(playlistPrivada));
        when(playlistTrackRepository.findByPlaylistIdOrderByPosition(10L)).thenReturn(java.util.List.of());

        assertDoesNotThrow(() -> service.getDetail(10L, dono));
    }

    @Test
    @DisplayName("Outro utilizador não vê uma playlist privada, recebe 404")
    void outroNaoVePlaylistPrivada() {
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(playlistPrivada));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getDetail(10L, outro));

        assertEquals(404, ex.getStatusCode().value());
        verify(playlistTrackRepository, never()).findByPlaylistIdOrderByPosition(any());
    }

    @Test
    @DisplayName("Outro utilizador vê uma playlist pública")
    void outroVePlaylistPublica() {
        playlistPrivada.setPublic(true);
        when(playlistRepository.findById(10L)).thenReturn(Optional.of(playlistPrivada));
        when(playlistTrackRepository.findByPlaylistIdOrderByPosition(10L)).thenReturn(java.util.List.of());
        when(savedPlaylistRepository.existsByUserIdAndPlaylistId(2L, 10L)).thenReturn(false);

        assertDoesNotThrow(() -> service.getDetail(10L, outro));
    }
}
