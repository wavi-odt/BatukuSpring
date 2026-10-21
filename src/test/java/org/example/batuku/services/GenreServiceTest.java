package org.example.batuku.services;

import org.example.batuku.domain.Genre;
import org.example.batuku.dto.GenreResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock GenreRepository  genreRepository;
    @Mock TrackRepository  trackRepository;
    @Mock LikeRepository   likeRepository;
    @Mock AlbumRepository  albumRepository;
    @Mock AlbumService     albumService;
    @InjectMocks GenreService service;

    @Test
    @DisplayName("getTracksByGenre lança NoSuchElementException para género inexistente")
    void getTracksByGenreNaoExiste() {
        when(genreRepository.existsById(99L)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> service.getTracksByGenre(99L));
    }

    @Test
    @DisplayName("getTracksByGenre devolve lista vazia quando género não tem faixas")
    void getTracksByGenreVazio() {
        when(genreRepository.existsById(1L)).thenReturn(true);
        when(trackRepository.findByGenreIdAndIsPublishedTrueOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        var result = service.getTracksByGenre(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getReleasesByGenre lança NoSuchElementException para género inexistente")
    void getReleasesByGenreNaoExiste() {
        when(genreRepository.existsById(99L)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> service.getReleasesByGenre(99L));
    }

    @Test
    @DisplayName("getAllGenres devolve mapa com chaves mundiais e caboverde")
    void getAllGenres() {
        when(genreRepository.countTracksByGenre()).thenReturn(List.of());
        when(genreRepository.findAllByCaboverdeanOrderByNameAsc(false)).thenReturn(List.of());
        when(genreRepository.findAllByCaboverdeanOrderByNameAsc(true)).thenReturn(List.of());

        Map<String, List<GenreResponse>> result = service.getAllGenres();

        assertTrue(result.containsKey("mundiais"));
        assertTrue(result.containsKey("caboverde"));
    }
}
