package org.example.batuku.config;

import org.example.batuku.domain.Badge;
import org.example.batuku.repository.BadgeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Cria os badges de gamificação quando a aplicação arranca.
 * Idempotente, só insere se ainda não existir (verifica pelo nome único).
 *
 * Badges organizados por pontos necessários (pointsRequired):
 *   O utilizador desbloqueia o badge automaticamente ao atingir o threshold
 *   de pontos totais acumulados na plataforma.
 *
 * Pontos ganhos por ação (referência para GamificationService):
 *   PLAY    = 10 pts   LIKE      = 5 pts
 *   COMMENT = 15 pts   FOLLOW    = 8 pts
 *   SHARE   = 20 pts   STREAK    = 25 pts/dia
 */
@Configuration
public class SeedBadges {

    @Bean
    @Order(4)
    CommandLineRunner seedBadgesRunner(BadgeRepository badgeRepository) {
        return args -> {
            upsert(badgeRepository, "Madrugador",
                    "Ouviste música antes das 7h em 10 dias diferentes",
                    "🌅", 100);

            upsert(badgeRepository, "Streak 12",
                    "Mantiveste um streak de 12 dias seguidos",
                    "🔥", 200);

            upsert(badgeRepository, "Explorador",
                    "Ouviste música de 50 artistas diferentes",
                    "🧭", 350);

            upsert(badgeRepository, "Apoiante",
                    "Seguiste 20 artistas na plataforma",
                    "❤️", 500);

            upsert(badgeRepository, "Curador",
                    "Uma das tuas playlists atingiu 100 likes",
                    "⭐", 700);

            upsert(badgeRepository, "Top 100",
                    "Entraste no top 100 do ranking semanal",
                    "👑", 1000);

            upsert(badgeRepository, "Fã Dedicado",
                    "Ouviste 100 horas de música no Batuku",
                    "🎧", 1500);

            upsert(badgeRepository, "Streak 30",
                    "Mantiveste um streak de 30 dias seguidos",
                    "🔥", 2000);

            upsert(badgeRepository, "Influenciador",
                    "Uma das tuas playlists atingiu 1000 likes",
                    "⭐", 4000);

            upsert(badgeRepository, "Lenda",
                    "Atingiste o nível 10 na plataforma",
                    "💎", 5000);

            // Remover badges obsoletos
            badgeRepository.findByName("Embaixador").ifPresent(badgeRepository::delete);
            badgeRepository.findByName("Concertos").ifPresent(badgeRepository::delete);

            System.out.println("Badges do Batuku verificados/criados.");
        };
    }

    private void upsert(BadgeRepository repo, String nome, String descricao, String icone, int pontos) {
        repo.findByName(nome).ifPresentOrElse(b -> {
            b.setDescription(descricao);
            b.setIconUrl(icone);
            repo.save(b);
        }, () -> {
            Badge b = new Badge();
            b.setName(nome);
            b.setDescription(descricao);
            b.setIconUrl(icone);
            b.setPointsRequired(pontos);
            repo.save(b);
        });
    }
}
