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
            criarSeNaoExistir(badgeRepository, "Madrugador",
                    "Ouviste música antes das 7h por 10 dias",
                    "🌅", 100);

            criarSeNaoExistir(badgeRepository, "Streak 12",
                    "12 dias seguidos a ouvir música no Batuku",
                    "🔥", 200);

            criarSeNaoExistir(badgeRepository, "Explorador",
                    "Ouviste música de 50 artistas diferentes",
                    "🧭", 350);

            criarSeNaoExistir(badgeRepository, "Apoiante",
                    "Seguiste 20 artistas na plataforma",
                    "❤️", 500);

            criarSeNaoExistir(badgeRepository, "Curador",
                    "Criaste uma playlist com 100 ou mais likes",
                    "⭐", 700);

            criarSeNaoExistir(badgeRepository, "Top 100",
                    "Entraste no top 100 do ranking semanal",
                    "👑", 1000);

            criarSeNaoExistir(badgeRepository, "Fã Dedicado",
                    "Ouviste 100 horas de música no Batuku",
                    "🎧", 1500);

            criarSeNaoExistir(badgeRepository, "Streak 30",
                    "30 dias seguidos a ouvir música no Batuku",
                    "🔥", 2000);

            criarSeNaoExistir(badgeRepository, "Embaixador",
                    "Convidaste 5 amigos para se juntarem ao Batuku",
                    "🧭", 2500);

            criarSeNaoExistir(badgeRepository, "Concertos",
                    "Participaste em 5 eventos do Batuku",
                    "🎟️", 3000);

            criarSeNaoExistir(badgeRepository, "Influenciador",
                    "Criaste uma playlist com 1000 ou mais likes",
                    "⭐", 4000);

            criarSeNaoExistir(badgeRepository, "Lenda",
                    "Atingiste o nível 10 na plataforma",
                    "💎", 5000);

            System.out.println("Badges do Batuku verificados/criados.");
        };
    }

    private void criarSeNaoExistir(BadgeRepository repo,
                                   String nome,
                                   String descricao,
                                   String icone,
                                   int pontosNecessarios) {
        if (!repo.existsByName(nome)) {
            Badge b = new Badge();
            b.setName(nome);
            b.setDescription(descricao);
            b.setIconUrl(icone);
            b.setPointsRequired(pontosNecessarios);
            repo.save(b);
        }
    }
}
