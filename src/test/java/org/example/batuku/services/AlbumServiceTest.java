package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.Album;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.AlbumTrackRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida de um lancamento: rascunho (DRAFT), agendado (SCHEDULED) e
 * publicado (PUBLISHED), com as regras de propriedade e de estado.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlbumServiceTest {

    @Mock AlbumRepository albumRepository;
    @Mock AlbumTrackRepository albumTrackRepository;
    @Mock TrackService trackService;
    @Mock LikeRepository likeRepository;
    @Mock CommentRepository commentRepository;
    @InjectMocks AlbumService service;

    private User artist;
    private ArtistProfile perfil;
    private Album album;

    @BeforeEach
    void setUp() {
        artist = TestData.user(2L, User.UserRole.ARTIST);
        perfil = TestData.artistProfile(20L, artist);

        album = TestData.withId(new Album(), 7L);
        album.setTitle("Disco");
        album.setArtistProfile(perfil);
        album.setAlbumType(Album.AlbumType.ALBUM);
        album.setStatus(Album.Status.DRAFT);

        when(albumRepository.findById(7L)).thenReturn(Optional.of(album));
        when(trackService.getArtistProfileOrThrow(artist)).thenReturn(perfil);
        when(albumTrackRepository.countByAlbumId(7L)).thenReturn(2L);
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static int estado(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    @DisplayName("Com data futura, o lancamento fica agendado")
    void dataFuturoAgenda() {
        LocalDateTime quando = LocalDateTime.now().plusDays(2);

        ReleaseResponse r = service.publish(7L, artist, quando);

        assertEquals(Album.Status.SCHEDULED, album.getStatus());
        assertEquals(quando, album.getScheduledAt());
        assertEquals("SCHEDULED", r.getStatus());
    }

    @Test
    @DisplayName("Sem data, o lancamento e publicado de imediato")
    void semDataPublicaDeImediato() {
        ReleaseResponse r = service.publish(7L, artist, null);

        assertEquals(Album.Status.PUBLISHED, album.getStatus());
        assertNull(album.getScheduledAt());
        assertEquals("PUBLISHED", r.getStatus());
    }

    @Test
    @DisplayName("Com data no passado, o lancamento e publicado de imediato")
    void dataPassadaPublicaDeImediato() {
        service.publish(7L, artist, LocalDateTime.now().minusHours(1));

        assertEquals(Album.Status.PUBLISHED, album.getStatus());
    }

    @Test
    @DisplayName("Um lancamento sem faixas nao pode ser publicado (422)")
    void semFaixasNaoPublica() {
        when(albumTrackRepository.countByAlbumId(7L)).thenReturn(0L);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.publish(7L, artist, null));

        assertEquals(422, estado(e));
        assertEquals(Album.Status.DRAFT, album.getStatus());
        verify(albumRepository, never()).save(any(Album.class));
    }

    @Test
    @DisplayName("Um lancamento ja publicado nao pode ser publicado outra vez (409)")
    void jaPublicadoNaoRepublica() {
        album.setStatus(Album.Status.PUBLISHED);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.publish(7L, artist, null));

        assertEquals(409, estado(e));
    }

    @Test
    @DisplayName("So o dono do lancamento o pode publicar (403)")
    void outroArtistaNaoPublica() {
        User outro = TestData.user(3L, User.UserRole.ARTIST);
        when(trackService.getArtistProfileOrThrow(outro)).thenReturn(TestData.artistProfile(21L, outro));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.publish(7L, outro, null));

        assertEquals(403, estado(e));
        assertEquals(Album.Status.DRAFT, album.getStatus());
    }

    @Test
    @DisplayName("Um lancamento inexistente devolve 404")
    void lancamentoInexistente() {
        when(albumRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.publish(99L, artist, null));

        assertEquals(404, estado(e));
    }

    @Test
    @DisplayName("Nao se acrescentam faixas a um lancamento ja publicado (409)")
    void naoAcrescentaFaixasAPublicado() {
        album.setStatus(Album.Status.PUBLISHED);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.addTrack(7L, artist, "Faixa", null, mock(MultipartFile.class)));

        assertEquals(409, estado(e));
    }

    @Test
    @DisplayName("So o dono acrescenta faixas ao rascunho (403)")
    void outroArtistaNaoAcrescentaFaixas() {
        User outro = TestData.user(3L, User.UserRole.ARTIST);
        when(trackService.getArtistProfileOrThrow(outro)).thenReturn(TestData.artistProfile(21L, outro));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.addTrack(7L, outro, "Faixa", null, mock(MultipartFile.class)));

        assertEquals(403, estado(e));
    }

    @Test
    @DisplayName("So o dono pode eliminar o lancamento (403)")
    void outroArtistaNaoElimina() {
        User outro = TestData.user(3L, User.UserRole.ARTIST);
        when(trackService.getArtistProfileOrThrow(outro)).thenReturn(TestData.artistProfile(21L, outro));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.deleteRelease(7L, outro));

        assertEquals(403, estado(e));
        verify(albumRepository, never()).delete(any(Album.class));
    }

    @Test
    @DisplayName("A tarefa periodica publica os lancamentos agendados cuja hora ja passou")
    void tarefaPublicaLancamentosVencidos() {
        Album a1 = agendado(11L);
        Album a2 = agendado(12L);
        when(albumRepository.findByScheduledAtBeforeAndStatus(any(LocalDateTime.class), eq(Album.Status.SCHEDULED)))
                .thenReturn(List.of(a1, a2));

        service.publishDueAlbums();

        assertEquals(Album.Status.PUBLISHED, a1.getStatus());
        assertEquals(Album.Status.PUBLISHED, a2.getStatus());
        verify(albumRepository, times(2)).save(any(Album.class));
    }

    @Test
    @DisplayName("Sem lancamentos vencidos, a tarefa periodica nao altera nada")
    void tarefaSemLancamentosVencidos() {
        when(albumRepository.findByScheduledAtBeforeAndStatus(any(LocalDateTime.class), eq(Album.Status.SCHEDULED)))
                .thenReturn(List.of());

        service.publishDueAlbums();

        verify(albumRepository, never()).save(any(Album.class));
    }

    private Album agendado(Long id) {
        Album a = TestData.withId(new Album(), id);
        a.setArtistProfile(perfil);
        a.setAlbumType(Album.AlbumType.ALBUM);
        a.setStatus(Album.Status.SCHEDULED);
        a.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        return a;
    }

    @ParameterizedTest(name = "tipo \"{0}\" -> {1}")
    @CsvSource({"ep,EP", "EP,EP", "single,SINGLE", "Mixtape 2024,MIXTAPE", "album,ALBUM", "outro,ALBUM"})
    @DisplayName("O tipo de lancamento e interpretado a partir do texto recebido")
    void tipoDeLancamento(String texto, String esperado) {
        ReleaseResponse r = service.createDraft(artist, "Novo", null, texto, null);

        assertEquals(esperado, r.getAlbumType());
        assertEquals("DRAFT", r.getStatus());
    }

    @Test
    @DisplayName("Sem tipo indicado, o lancamento e um album")
    void tipoPorOmissao() {
        ReleaseResponse r = service.createDraft(artist, "Novo", null, null, null);

        assertNotNull(r);
        assertEquals("ALBUM", r.getAlbumType());
    }
}
