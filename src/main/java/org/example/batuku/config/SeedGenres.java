package org.example.batuku.config;

import org.example.batuku.domain.Genre;
import org.example.batuku.repository.GenreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class SeedGenres {

    @Bean
    @Order(6)
    CommandLineRunner seedGenresRunner(GenreRepository genreRepository) {
        return args -> {
            // Apaga géneros eliminados
            genreRepository.findByNameIgnoreCase("Afrobeats").ifPresent(genreRepository::delete);
            genreRepository.findByNameIgnoreCase("R&B & Soul").ifPresent(genreRepository::delete);
            genreRepository.findByNameIgnoreCase("Batukó").ifPresent(genreRepository::delete);
            genreRepository.findByNameIgnoreCase("Batuque").ifPresent(genreRepository::delete);
            genreRepository.findByNameIgnoreCase("Finaçom").ifPresent(genreRepository::delete);
            genreRepository.findByNameIgnoreCase("Mazurca").ifPresent(genreRepository::delete);

            // Géneros mundiais
            seed(genreRepository, "Pop",         340, false);
            seed(genreRepository, "Hip-Hop",     28,  false);
            seed(genreRepository, "Soul",        290, false);
            seed(genreRepository, "Electrónica", 200, false);
            seed(genreRepository, "Afrobeat",    22,  false);
            seed(genreRepository, "Afrohouse",   35,  false);
            seed(genreRepository, "Afrotech",    195, false);
            seed(genreRepository, "Amapiano",    120, false);
            seed(genreRepository, "Gqom",        170, false);
            seed(genreRepository, "Trap",        270, false);
            seed(genreRepository, "Boom Bap",    30,  false);
            seed(genreRepository, "Drill",       10,  false);
            seed(genreRepository, "R&B",         300, false);
            seed(genreRepository, "Dancehall",   150, false);
            seed(genreRepository, "Reggae",      130, false);
            seed(genreRepository, "Jazz",        240, false);
            seed(genreRepository, "Rock",        5,   false);
            seed(genreRepository, "Latin",       15,  false);
            seed(genreRepository, "Kizomba",     320, false);
            seed(genreRepository, "Gospel",      55,  false);
            seed(genreRepository, "Clássico",    260, false);

            // Géneros cabo-verdianos
            seed(genreRepository, "Funaná",    14,  true);
            seed(genreRepository, "Morna",     220, true);
            seed(genreRepository, "Coladeira", 42,  true);
            seed(genreRepository, "Cabo Love", 145, true);
            seed(genreRepository, "Tabanka",   8,   true);
            seed(genreRepository, "Batuku",    185, true);
            seed(genreRepository, "Kotxi Po",  95,  true);

            System.out.println("Géneros do Batuku verificados/criados.");
        };
    }

    private void seed(GenreRepository repo, String name, int hue, boolean caboverdean) {
        if (!repo.existsByName(name)) {
            Genre g = new Genre();
            g.setName(name);
            g.setHue(hue);
            g.setCaboverdean(caboverdean);
            repo.save(g);
        }
    }
}
