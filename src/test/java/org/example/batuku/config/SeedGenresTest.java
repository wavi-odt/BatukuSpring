package org.example.batuku.config;

import org.example.batuku.domain.Genre;
import org.example.batuku.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.CommandLineRunner;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Carga inicial de generos musicais: 21 internacionais e 7 cabo-verdianos,
 * idempotente (so cria o que falta) e com remocao de generos descontinuados.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeedGenresTest {

    @Mock GenreRepository genreRepository;

    private CommandLineRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SeedGenres().seedGenresRunner(genreRepository);
    }

    private List<Genre> criados() {
        ArgumentCaptor<Genre> captor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository, times(28)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("Numa base vazia sao criados 28 generos")
    void baseVazia() throws Exception {
        runner.run();

        assertEquals(28, criados().size());
    }

    @Test
    @DisplayName("Sete dos generos criados sao cabo-verdianos")
    void generosCaboVerdianos() throws Exception {
        runner.run();

        Set<String> caboVerdianos = criados().stream()
                .filter(Genre::isCaboverdean).map(Genre::getName).collect(Collectors.toSet());
        assertEquals(Set.of("Funaná", "Morna", "Coladeira", "Cabo Love", "Tabanka", "Batuku", "Kotxi Po"),
                caboVerdianos);
    }

    @Test
    @DisplayName("Cada genero tem um nome unico e uma cor (hue) valida")
    void nomesUnicosECoresValidas() throws Exception {
        runner.run();

        List<Genre> generos = criados();
        assertEquals(28, generos.stream().map(Genre::getName).distinct().count());
        assertTrue(generos.stream().allMatch(g -> g.getHue() >= 0 && g.getHue() < 360));
    }

    @Test
    @DisplayName("Executar de novo nao duplica os generos que ja existem")
    void idempotente() throws Exception {
        when(genreRepository.existsByName(any())).thenReturn(true);

        runner.run();

        verify(genreRepository, never()).save(any(Genre.class));
    }

    @Test
    @DisplayName("So cria os generos em falta")
    void soCriaOsEmFalta() throws Exception {
        when(genreRepository.existsByName(any())).thenReturn(true);
        when(genreRepository.existsByName("Morna")).thenReturn(false);

        runner.run();

        ArgumentCaptor<Genre> captor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository).save(captor.capture());
        assertEquals("Morna", captor.getValue().getName());
        assertTrue(captor.getValue().isCaboverdean());
    }

    @Test
    @DisplayName("Os generos descontinuados sao removidos")
    void removeDescontinuados() throws Exception {
        Genre antigo = new Genre();
        antigo.setName("Mazurca");
        when(genreRepository.findByNameIgnoreCase("Mazurca")).thenReturn(Optional.of(antigo));

        runner.run();

        verify(genreRepository).delete(antigo);
    }

    @Test
    @DisplayName("Sem generos descontinuados, nada e removido")
    void nadaARemover() throws Exception {
        runner.run();

        verify(genreRepository, never()).delete(any(Genre.class));
    }
}
