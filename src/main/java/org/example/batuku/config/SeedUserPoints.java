package org.example.batuku.config;

import org.example.batuku.domain.User;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.GamificationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Garante que todos os utilizadores existentes têm um registo UserPoints.
 * Corre após o SeedBadges (Order 4). Idempotente — o inicializarPontos
 * já verifica se o registo existe antes de criar.
 */
@Configuration
public class SeedUserPoints {

    @Bean
    @Order(5)
    CommandLineRunner seedUserPointsRunner(UserRepository userRepository,
                                          GamificationService gamificationService) {
        return args -> {
            List<User> users = userRepository.findAll();
            int criados = 0;
            for (User user : users) {
                try {
                    gamificationService.inicializarPontos(user);
                    criados++;
                } catch (Exception ignored) {}
            }
            if (criados > 0) {
                System.out.println("UserPoints inicializados para " + criados + " utilizador(es) existente(s).");
            }
        };
    }
}
