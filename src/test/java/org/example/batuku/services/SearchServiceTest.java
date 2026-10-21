package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.SearchResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock ArtistProfileRepository artistProfileRepository;
    @Mock TrackRepository         trackRepository;
    @Mock UserRepository          userRepository;
    @InjectMocks SearchService service;

    @Test
    @DisplayName("search devolve listas vazias quando query tem menos de 2 caracteres")
    void searchQueryCurta() {
        SearchResponse r = service.search("a");

        assertTrue(r.users().isEmpty());
        assertTrue(r.tracks().isEmpty());
        assertTrue(r.artists().isEmpty());
        verifyNoInteractions(artistProfileRepository, trackRepository, userRepository);
    }

    @Test
    @DisplayName("search devolve listas vazias quando query é nula")
    void searchQueryNula() {
        SearchResponse r = service.search(null);

        assertTrue(r.users().isEmpty());
        verifyNoInteractions(artistProfileRepository, trackRepository, userRepository);
    }

    @Test
    @DisplayName("search devolve resultados quando query tem 2 ou mais caracteres")
    void searchValida() {
        User artist = TestData.user(1L, User.UserRole.ARTIST);
        ArtistProfile ap = TestData.artistProfile(10L, artist);
        ap.setGenres(List.of("Batuku"));

        Track track = TestData.track(5L, ap);
        track.setDurationMs(180000);

        when(artistProfileRepository.findTop5ByNameContainingIgnoreCase("Ba")).thenReturn(List.of());
        when(userRepository.findTop5ByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase("Ba", "Ba"))
                .thenReturn(List.of(artist));
        when(artistProfileRepository.findByUserIdIn(any())).thenReturn(List.of(ap));
        when(trackRepository.findTop5ByTitleContainingIgnoreCase("Ba")).thenReturn(List.of(track));

        SearchResponse r = service.search("Ba");

        assertNotNull(r);
        assertEquals(1, r.users().size());
        assertEquals(1, r.tracks().size());
    }
}
