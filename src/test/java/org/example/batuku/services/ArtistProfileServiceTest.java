package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistDetailResponse;
import org.example.batuku.dto.ArtistFollowResponse;
import org.example.batuku.dto.ArtistOptionsResponse;
import org.example.batuku.dto.SimilarArtistResponse;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.LanguageRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.LocationRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UnmappedGenreRepository;
import org.example.batuku.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistProfileServiceTest {

    @Mock ArtistProfileRepository artistProfileRepository;
    @Mock SpotifyClient spotifyClient;
    @Mock TrackRepository trackRepository;
    @Mock GenreRepository genreRepository;
    @Mock UnmappedGenreRepository unmappedGenreRepository;
    @Mock UserRepository userRepository;
    @Mock LikeRepository likeRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock PlayRepository playRepository;
    @Mock LanguageRepository languageRepository;
    @Mock LocationRepository locationRepository;
    @InjectMocks ArtistProfileService service;

    private User owner;
    private ArtistProfile profile;

    @BeforeEach
    void setUp() {
        owner = TestData.user(1L, User.UserRole.ARTIST);
        profile = TestData.artistProfile(10L, owner);
        profile.setClaimed(true);
    }

    @Test
    @DisplayName("findByEmail devolve Optional vazio se o utilizador não existe")
    void findByEmailUtilizadorInexistente() {
        when(userRepository.findByEmail("nao@existe.pt")).thenReturn(Optional.empty());

        assertTrue(service.findByEmail("nao@existe.pt").isEmpty());
    }

    @Test
    @DisplayName("findByEmail devolve Optional vazio se o utilizador não tem perfil de artista")
    void findByEmailSemPerfil() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertTrue(service.findByEmail(owner.getEmail()).isEmpty());
    }

    @Test
    @DisplayName("findByEmail devolve o perfil quando existe")
    void findByEmailComPerfil() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        Optional<ArtistProfile> result = service.findByEmail(owner.getEmail());

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
    }

    @Test
    @DisplayName("updateName rejeita nome vazio com IllegalArgumentException")
    void updateNameVazio() {
        assertThrows(IllegalArgumentException.class, () -> service.updateName(profile, ""));
        verify(artistProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateName guarda o nome e devolve o valor aparado")
    void updateNameValido() {
        when(artistProfileRepository.save(any())).thenReturn(profile);

        String saved = service.updateName(profile, "  Novo Nome  ");

        assertEquals("Novo Nome", saved);
        assertEquals("Novo Nome", profile.getName());
        verify(artistProfileRepository).save(profile);
    }

    @Test
    @DisplayName("updateLocation rejeita localização não canónica")
    void updateLocationInvalida() {
        when(locationRepository.existsByValue("Lua")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.updateLocation(profile, "Lua"));
        verify(artistProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLocation aceita localização válida")
    void updateLocationValida() {
        when(locationRepository.existsByValue("Lisboa, Portugal")).thenReturn(true);
        when(artistProfileRepository.save(any())).thenReturn(profile);

        service.updateLocation(profile, "Lisboa, Portugal");

        assertEquals("Lisboa, Portugal", profile.getLocation());
        verify(artistProfileRepository).save(profile);
    }

    @Test
    @DisplayName("updateGenres rejeita géneros não reconhecidos")
    void updateGenresInvalidos() {
        org.example.batuku.domain.Genre funk = new org.example.batuku.domain.Genre();
        funk.setName("Funana");
        when(genreRepository.findAll()).thenReturn(List.of(funk));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateGenres(profile, List.of("Funana", "GeneroInexistente")));
        verify(artistProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateGenres aceita géneros válidos")
    void updateGenresValidos() {
        org.example.batuku.domain.Genre funk = new org.example.batuku.domain.Genre();
        funk.setName("Funana");
        when(genreRepository.findAll()).thenReturn(List.of(funk));
        when(artistProfileRepository.save(any())).thenReturn(profile);

        service.updateGenres(profile, List.of("Funana"));

        assertEquals(List.of("Funana"), profile.getGenres());
    }

    @Test
    @DisplayName("updateLanguages rejeita línguas não canónicas")
    void updateLanguagesInvalidas() {
        org.example.batuku.domain.Language pt = new org.example.batuku.domain.Language();
        pt.setName("Português");
        when(languageRepository.findAll()).thenReturn(List.of(pt));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateLanguages(profile, List.of("Klingoniano")));
    }

    @Test
    @DisplayName("updateLinks filtra entradas sem kind e guarda as restantes")
    void updateLinksFiltraVazios() {
        when(artistProfileRepository.save(any())).thenReturn(profile);

        service.updateLinks(profile, List.of(
                Map.of("kind", "instagram", "handle", "@artista"),
                Map.of("kind", "", "handle", "nada")
        ));

        assertNotNull(profile.getLinks());
        assertEquals(1, profile.getLinks().size());
        assertEquals("instagram", profile.getLinks().get(0).getKind());
    }

    @Test
    @DisplayName("getArtistDetail devolve detalhe completo do artista")
    void getArtistDetailCompleto() {
        when(artistProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(trackRepository.findByArtistProfileId(10L)).thenReturn(List.of());
        when(artistFollowRepository.countByArtistProfileId(10L)).thenReturn(100L);
        when(playRepository.countDistinctListenersByArtistSince(eq(10L), any(LocalDateTime.class))).thenReturn(50L);

        ArtistDetailResponse detail = service.getArtistDetail(10L);

        assertEquals(10L, detail.id());
        assertEquals(100L, detail.followers());
        assertEquals(50L, detail.monthlyListeners());
        assertEquals(0, detail.tracksCount());
    }

    @Test
    @DisplayName("getArtistDetail lança NoSuchElementException para id inexistente")
    void getArtistDetailInexistente() {
        when(artistProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> service.getArtistDetail(99L));
    }

    @Test
    @DisplayName("getOptions devolve listas de géneros, línguas e localizações")
    void getOptionsConteudo() {
        org.example.batuku.domain.Genre g = new org.example.batuku.domain.Genre();
        g.setName("Batuku");
        when(genreRepository.findAll()).thenReturn(List.of(g));
        when(languageRepository.findAll()).thenReturn(List.of());
        when(locationRepository.findAll()).thenReturn(List.of());

        ArtistOptionsResponse opts = service.getOptions();

        assertEquals(List.of("Batuku"), opts.genres());
        assertTrue(opts.languages().isEmpty());
        assertTrue(opts.locations().isEmpty());
    }

    @Test
    @DisplayName("getSuggestedArtists sem utilizador autenticado devolve artistas com ouvintes ≥ 1")
    void getSuggestedSemUtilizador() {
        ArtistProfile a = TestData.artistProfile(20L, owner);
        when(artistProfileRepository.findAll()).thenReturn(List.of(a));
        when(artistFollowRepository.countByArtistProfileId(20L)).thenReturn(5L);
        when(playRepository.countByTrackArtistProfileIdAndPlayedAtAfter(eq(20L), any())).thenReturn(10L);
        when(playRepository.countDistinctListenersByArtistSince(eq(20L), any())).thenReturn(2L);
        when(likeRepository.countByTrackArtistProfileIdAndCreatedAtAfter(eq(20L), any())).thenReturn(1L);
        when(artistFollowRepository.countByArtistProfileIdAndCreatedAtAfter(eq(20L), any())).thenReturn(0L);

        List<ArtistFollowResponse> result = service.getSuggestedArtists(null);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).id());
    }

    @Test
    @DisplayName("getSuggestedArtists exclui artistas já seguidos pelo utilizador")
    void getSuggestedExcluiSeguidos() {
        org.example.batuku.domain.ArtistFollow af = new org.example.batuku.domain.ArtistFollow();
        af.setArtistProfile(profile);
        when(artistFollowRepository.findByFollowerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(af));
        when(artistProfileRepository.findAll()).thenReturn(List.of(profile));

        List<ArtistFollowResponse> result = service.getSuggestedArtists(owner);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getSimilarArtists usa fallback para artistas mais ouvidos se não há géneros em comum")
    void getSimilarSemGenerosEmComum() {
        ArtistProfile other = TestData.artistProfile(30L, TestData.user(3L, User.UserRole.ARTIST));
        profile.setGenres(List.of("Funana"));
        other.setGenres(List.of("Jazz"));

        when(artistProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.findAll()).thenReturn(List.of(profile, other));
        when(playRepository.countTotalPlaysByArtist(30L)).thenReturn(50L);

        List<SimilarArtistResponse> result = service.getSimilarArtists(10L, 5);

        assertEquals(1, result.size());
        assertEquals(30L, result.get(0).id());
    }
}
