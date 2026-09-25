package org.example.batuku.config;

import org.example.batuku.domain.Language;
import org.example.batuku.repository.LanguageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class SeedLanguages {

    @Bean
    @Order(7)
    CommandLineRunner seedLanguagesRunner(LanguageRepository languageRepository) {
        return args -> {
            seed(languageRepository, "Crioulo (CV)");
            seed(languageRepository, "Português");
            seed(languageRepository, "Inglês");
            seed(languageRepository, "Francês");
            seed(languageRepository, "Espanhol");
            seed(languageRepository, "Holandês");
            seed(languageRepository, "Alemão");
            seed(languageRepository, "Italiano");

            System.out.println("Línguas do Batuku verificadas/criadas.");
        };
    }

    private void seed(LanguageRepository repo, String name) {
        if (!repo.existsByName(name)) {
            Language l = new Language();
            l.setName(name);
            repo.save(l);
        }
    }
}
