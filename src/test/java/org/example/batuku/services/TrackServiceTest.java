package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.config.StorageProperties;
import org.example.batuku.domain.ArtistFollow;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.*;
import org.example.batuku.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock TrackRepository          trackRepository;
    @Mock ArtistProfileRepository  artistProfileRepository;
    @Mock GenreRepository          genreRepository;
    @Mock FileStorageService       storageService;
    @Mock StorageProperties        storageProperties;
    @Mock AlbumTrackRepository     albumTrackRepository;
    @Mock AlbumRepository          albumRepository;
    @Mock LikeRepository           likeRepository;
    @Mock CommentRepository        commentRepository;
    @Mock PlayRepository           playRepository;
    @Mock PlaylistTrackRepository  playlistTrackRepository;
    @Mock ShareRepository          shareRepository;
    @Mock ArtistFollowRepository   artistFollowRepository;
    @InjectMocks TrackService service;

    private User artist;
    private ArtistProfile profile;
    private Track track;

    @BeforeEach
    void setUp() {
        artist  = TestData.user(1L, User.UserRole.ARTIST);
        profile = TestData.artistProfile(10L, artist);
        track   = TestData.track(5L, profile);
        track.setSource(Track.TrackSource.UPLOAD);
    }

    @Test
    @DisplayName("listAllWithLikes devolve lista com likeCount correto")
    void listAllWithLikes() {
        when(trackRepository.findAllWithRelations()).thenReturn(List.of(track));
        when(likeRepository.countGroupedByTrack()).thenReturn(List.<Object[]>of(new Object[]{5L, 7L}));

        List<TrackResponse> result = service.listAllWithLikes();

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getLikeCount());
    }

    @Test
    @DisplayName("listByArtistWithStats devolve faixas com stats completas")
    void listByArtistWithStats() {
        when(trackRepository.findByArtistProfileId(10L)).thenReturn(List.of(track));
        when(likeRepository.countByTrackId(5L)).thenReturn(3L);
        when(playRepository.countByTrackId(5L)).thenReturn(10L);
        when(commentRepository.countByTrackId(5L)).thenReturn(2L);
        when(albumTrackRepository.existsByTrackId(5L)).thenReturn(false);

        List<TrackResponse> result = service.listByArtistWithStats(10L);

        assertEquals(1, result.size());
        TrackResponse r = result.get(0);
        assertEquals(3L, r.getLikeCount());
        assertEquals(10L, r.getPlayCount());
        assertEquals(2L, r.getCommentCount());
        assertFalse(r.isBelongsToRelease());
    }

    @Test
    @DisplayName("listByArtistWithStats marca belongsToRelease quando faixa está num álbum")
    void listByArtistWithStatsBelongsToRelease() {
        when(trackRepository.findByArtistProfileId(10L)).thenReturn(List.of(track));
        when(likeRepository.countByTrackId(5L)).thenReturn(0L);
        when(playRepository.countByTrackId(5L)).thenReturn(0L);
        when(commentRepository.countByTrackId(5L)).thenReturn(0L);
        when(albumTrackRepository.existsByTrackId(5L)).thenReturn(true);

        List<TrackResponse> result = service.listByArtistWithStats(10L);

        assertTrue(result.get(0).isBelongsToRelease());
    }

    @Test
    @DisplayName("getTrackWithStats devolve TrackResponse com stats completas")
    void getTrackWithStats() {
        when(trackRepository.findById(5L)).thenReturn(Optional.of(track));
        when(likeRepository.countByTrackId(5L)).thenReturn(4L);
        when(playRepository.countByTrackId(5L)).thenReturn(20L);
        when(commentRepository.countByTrackId(5L)).thenReturn(1L);
        when(albumTrackRepository.existsByTrackId(5L)).thenReturn(false);

        TrackResponse r = service.getTrackWithStats(5L);

        assertEquals(5L, r.getId());
        assertEquals(4L, r.getLikeCount());
        assertEquals(20L, r.getPlayCount());
        assertEquals(1L, r.getCommentCount());
        assertFalse(r.isBelongsToRelease());
    }

    @Test
    @DisplayName("getTrackWithStats lança exceção quando faixa não existe")
    void getTrackWithStatsNaoExiste() {
        when(trackRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.getTrackWithStats(99L));
    }

    @Test
    @DisplayName("getFollowingTracks devolve lista vazia se utilizador não segue ninguém")
    void getFollowingTracksVazio() {
        when(artistFollowRepository.findByFollowerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<TrackResponse> result = service.getFollowingTracks(artist);

        assertTrue(result.isEmpty());
        verify(trackRepository, never()).findTop20ByArtistProfileIdInAndIsPublishedTrueOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getFollowingTracks devolve faixas recentes dos artistas seguidos")
    void getFollowingTracksComSeguidos() {
        ArtistFollow follow = new ArtistFollow();
        follow.setArtistProfile(profile);
        when(artistFollowRepository.findByFollowerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(follow));
        when(trackRepository.findTop20ByArtistProfileIdInAndIsPublishedTrueOrderByCreatedAtDesc(List.of(10L)))
                .thenReturn(List.of(track));
        when(likeRepository.countByTrackId(5L)).thenReturn(2L);
        when(playRepository.countByTrackId(5L)).thenReturn(5L);

        List<TrackResponse> result = service.getFollowingTracks(artist);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getLikeCount());
        assertEquals(5L, result.get(0).getPlayCount());
    }
}
