package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Play;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.StatsResponse;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock ArtistProfileRepository artistProfileRepository;
    @Mock PlayRepository          playRepository;
    @Mock LikeRepository          likeRepository;
    @Mock ArtistFollowRepository  artistFollowRepository;
    @Mock TrackRepository         trackRepository;
    @Mock CommentRepository       commentRepository;
    @Mock GamificationService     gamificationService;
    @Mock ForecastService         forecastService;
    @Mock GroqClient              groqClient;
    @InjectMocks StatsService service;

    private User artist;
    private ArtistProfile profile;
    private Track track;

    @BeforeEach
    void setUp() {
        artist  = TestData.user(1L, User.UserRole.ARTIST);
        profile = TestData.artistProfile(10L, artist);
        track   = TestData.track(5L, profile);
    }

    @Test
    @DisplayName("findProfileByUserId devolve Optional vazio se sem perfil")
    void findProfileSemPerfil() {
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertTrue(service.findProfileByUserId(1L).isEmpty());
    }

    @Test
    @DisplayName("findProfileByUserId devolve o perfil quando existe")
    void findProfileComPerfil() {
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        assertTrue(service.findProfileByUserId(1L).isPresent());
    }

    @Test
    @DisplayName("getStats devolve StatsResponse com KPIs preenchidos")
    void getStatsKpis() {
        when(playRepository.countByTrackArtistProfileIdAndPlayedAtAfter(eq(10L), any())).thenReturn(100L);
        when(playRepository.countByTrackArtistProfileIdAndPlayedAtBetween(eq(10L), any(), any())).thenReturn(80L);
        when(likeRepository.countByTrackArtistProfileIdAndCreatedAtAfter(eq(10L), any())).thenReturn(20L);
        when(likeRepository.countByTrackArtistProfileIdAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(15L);
        when(artistFollowRepository.countByArtistProfileId(10L)).thenReturn(500L);
        when(artistFollowRepository.countByArtistProfileIdAndCreatedAtAfter(eq(10L), any())).thenReturn(5L);
        when(artistFollowRepository.countByArtistProfileIdAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(3L);
        when(commentRepository.countByArtistProfileAndCreatedAtAfter(eq(10L), any())).thenReturn(8L);
        when(commentRepository.countByArtistProfileAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(6L);
        when(playRepository.findDailyPlaysByArtist(eq(10L), any())).thenReturn(List.of());
        when(likeRepository.findDailyLikesByArtist(eq(10L), any())).thenReturn(List.of());
        when(artistFollowRepository.findDailyFollowsByArtist(eq(10L), any())).thenReturn(List.of());
        when(playRepository.findTopTracksByArtist(eq(10L), any())).thenReturn(List.of());
        when(trackRepository.findByArtistProfileId(10L)).thenReturn(List.of());
        when(playRepository.findSourceBreakdownByArtist(eq(10L), any())).thenReturn(List.of());
        when(playRepository.findCountryBreakdownByArtist(eq(10L), any())).thenReturn(List.of());
        when(playRepository.findAvgCompletionRateByArtist(eq(10L), any())).thenReturn(null);

        StatsResponse result = service.getStats(10L, "30d");

        assertEquals(100L, result.kpis().get("plays").value());
        assertEquals(500L, result.kpis().get("followers").value());
        assertEquals(30, result.dailyPlays().size());
    }

    @Test
    @DisplayName("getStats com período 7d devolve série com 7 pontos")
    void getStatsPeriodo7d() {
        stubEmptyStats(10L);
        StatsResponse result = service.getStats(10L, "7d");
        assertEquals(7, result.dailyPlays().size());
    }

    @Test
    @DisplayName("recordPlay regista play e devolve playId")
    void recordPlayDevolveId() {
        Play saved = TestData.withId(new Play(), 99L);
        when(playRepository.save(any())).thenReturn(saved);

        Optional<Long> playId = service.recordPlay(track, "PT", "home", null);

        assertTrue(playId.isPresent());
        assertEquals(99L, playId.get());
    }

    @Test
    @DisplayName("recordPlay não regista quando o ouvinte é o dono da faixa")
    void recordPlayDonoNaoContabiliza() {
        track.getArtistProfile().setUser(artist);

        Optional<Long> playId = service.recordPlay(track, null, null, artist);

        assertTrue(playId.isEmpty());
        verify(playRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordPlay devolve empty quando track é null")
    void recordPlayTrackNull() {
        assertTrue(service.recordPlay(null, null, null, null).isEmpty());
        verify(playRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePlay actualiza duração e não atribui pontos se já era fullPlay")
    void updatePlayJaEraFull() {
        Play play = new Play();
        play.setFullPlay(true);
        play.setUser(artist);
        play.setTrack(track);
        when(playRepository.findById(1L)).thenReturn(Optional.of(play));

        service.updatePlay(1L, 180, true);

        verify(playRepository).save(play);
        verifyNoInteractions(gamificationService);
    }

    @Test
    @DisplayName("updatePlay atribui pontos na primeira conclusão")
    void updatePlayPrimeiraConclusa() {
        Play play = new Play();
        play.setFullPlay(false);
        play.setUser(artist);
        play.setTrack(track);
        when(playRepository.findById(1L)).thenReturn(Optional.of(play));

        service.updatePlay(1L, 200, true);

        ArgumentCaptor<org.example.batuku.domain.PointTransaction.ActionType> captor =
                ArgumentCaptor.forClass(org.example.batuku.domain.PointTransaction.ActionType.class);
        verify(gamificationService).adicionarPontos(eq(artist), captor.capture(), eq(5L));
        assertEquals(org.example.batuku.domain.PointTransaction.ActionType.PLAY, captor.getValue());
    }

    private void stubEmptyStats(Long artistId) {
        when(playRepository.countByTrackArtistProfileIdAndPlayedAtAfter(eq(artistId), any())).thenReturn(0L);
        when(playRepository.countByTrackArtistProfileIdAndPlayedAtBetween(eq(artistId), any(), any())).thenReturn(0L);
        when(likeRepository.countByTrackArtistProfileIdAndCreatedAtAfter(eq(artistId), any())).thenReturn(0L);
        when(likeRepository.countByTrackArtistProfileIdAndCreatedAtBetween(eq(artistId), any(), any())).thenReturn(0L);
        when(artistFollowRepository.countByArtistProfileId(artistId)).thenReturn(0L);
        when(artistFollowRepository.countByArtistProfileIdAndCreatedAtAfter(eq(artistId), any())).thenReturn(0L);
        when(artistFollowRepository.countByArtistProfileIdAndCreatedAtBetween(eq(artistId), any(), any())).thenReturn(0L);
        when(commentRepository.countByArtistProfileAndCreatedAtAfter(eq(artistId), any())).thenReturn(0L);
        when(commentRepository.countByArtistProfileAndCreatedAtBetween(eq(artistId), any(), any())).thenReturn(0L);
        when(playRepository.findDailyPlaysByArtist(eq(artistId), any())).thenReturn(List.of());
        when(likeRepository.findDailyLikesByArtist(eq(artistId), any())).thenReturn(List.of());
        when(artistFollowRepository.findDailyFollowsByArtist(eq(artistId), any())).thenReturn(List.of());
        when(playRepository.findTopTracksByArtist(eq(artistId), any())).thenReturn(List.of());
        when(trackRepository.findByArtistProfileId(artistId)).thenReturn(List.of());
        when(playRepository.findSourceBreakdownByArtist(eq(artistId), any())).thenReturn(List.of());
        when(playRepository.findCountryBreakdownByArtist(eq(artistId), any())).thenReturn(List.of());
        when(playRepository.findAvgCompletionRateByArtist(eq(artistId), any())).thenReturn(null);
    }
}
