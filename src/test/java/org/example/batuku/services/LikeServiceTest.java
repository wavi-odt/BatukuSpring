package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Like;
import org.example.batuku.domain.Notification;
import org.example.batuku.domain.Playlist;
import org.example.batuku.domain.PlaylistTrack;
import org.example.batuku.domain.PointTransaction;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlaylistRepository;
import org.example.batuku.repository.PlaylistTrackRepository;
import org.example.batuku.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Gostos em faixas: idempotencia, pontos para o fa, notificacao ao artista
 * e sincronizacao com a playlist de favoritos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LikeServiceTest {

    @Mock LikeRepository likeRepository;
    @Mock TrackRepository trackRepository;
    @Mock PlaylistRepository playlistRepository;
    @Mock PlaylistTrackRepository playlistTrackRepository;
    @Mock GamificationService gamificationService;
    @Mock NotificationService notificationService;
    @InjectMocks LikeService service;

    private User fan;
    private User artist;
    private Track track;
    private Playlist favoritos;

    @BeforeEach
    void setUp() {
        fan = TestData.user(1L, User.UserRole.FAN);
        artist = TestData.user(2L, User.UserRole.ARTIST);
        ArtistProfile perfil = TestData.artistProfile(20L, artist);
        track = TestData.track(5L, perfil);

        favoritos = TestData.withId(new Playlist(), 10L);
        favoritos.setUser(fan);

        when(likeRepository.existsByUserIdAndTrackId(1L, 5L)).thenReturn(false);
        when(trackRepository.findById(5L)).thenReturn(Optional.of(track));
        when(playlistRepository.findByUserIdAndIsSystemGeneratedTrue(1L)).thenReturn(Optional.of(favoritos));
        when(playlistTrackRepository.existsByPlaylistIdAndTrackId(10L, 5L)).thenReturn(false);
        when(playlistTrackRepository.countByPlaylistId(10L)).thenReturn(3L);
    }

    @Test
    @DisplayName("Um gosto novo e registado, da pontos ao fa e notifica o artista")
    void gostoNovo() {
        service.like(fan, 5L);

        verify(likeRepository).save(any(Like.class));
        verify(gamificationService).adicionarPontos(fan, PointTransaction.ActionType.LIKE, 5L);
        verify(notificationService).notify(eq(artist), eq(Notification.NotificationType.LIKE),
                eq(5L), anyString());
    }

    @Test
    @DisplayName("A faixa gostada e acrescentada aos favoritos, na posicao seguinte")
    void acrescentaAosFavoritos() {
        service.like(fan, 5L);

        ArgumentCaptor<PlaylistTrack> captor = ArgumentCaptor.forClass(PlaylistTrack.class);
        verify(playlistTrackRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getPosition().intValue());   // ja havia 3 faixas
    }

    @Test
    @DisplayName("Repetir o mesmo gosto nao tem efeito (idempotente)")
    void gostoRepetidoNaoTemEfeito() {
        when(likeRepository.existsByUserIdAndTrackId(1L, 5L)).thenReturn(true);

        service.like(fan, 5L);

        verify(likeRepository, never()).save(any(Like.class));
        verifyNoInteractions(gamificationService, notificationService);
    }

    @Test
    @DisplayName("Gostar de uma faixa propria nao notifica o artista")
    void gostoNaPropriaFaixaNaoNotifica() {
        // o fa e o dono do perfil de artista da faixa
        track.getArtistProfile().setUser(fan);

        service.like(fan, 5L);

        verify(likeRepository).save(any(Like.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Gostar de uma faixa inexistente falha sem atribuir pontos")
    void faixaInexistente() {
        when(trackRepository.findById(99L)).thenReturn(Optional.empty());
        when(likeRepository.existsByUserIdAndTrackId(1L, 99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.like(fan, 99L));

        verify(likeRepository, never()).save(any(Like.class));
        verifyNoInteractions(gamificationService);
    }

    @Test
    @DisplayName("Retirar o gosto remove-o e tira a faixa dos favoritos")
    void retirarGosto() {
        service.unlike(fan, 5L);

        verify(likeRepository).deleteByUserIdAndTrackId(1L, 5L);
        verify(playlistTrackRepository).deleteByPlaylistIdAndTrackId(10L, 5L);
    }

    @Test
    @DisplayName("O estado indica se o utilizador gostou e quantos gostos a faixa tem")
    void estadoDoGosto() {
        when(likeRepository.existsByUserIdAndTrackId(1L, 5L)).thenReturn(true);
        when(likeRepository.countByTrackId(5L)).thenReturn(7L);

        LikeService.LikeStatus estado = service.status(fan, 5L);

        assertTrue(estado.liked());
        assertEquals(7L, estado.count());
    }

    @Test
    @DisplayName("O estado de quem nao gostou e liked = false")
    void estadoSemGosto() {
        when(likeRepository.countByTrackId(5L)).thenReturn(0L);

        LikeService.LikeStatus estado = service.status(fan, 5L);

        assertFalse(estado.liked());
        assertEquals(0L, estado.count());
    }
}
