package org.example.batuku.config;

import org.example.batuku.domain.Role;
import org.example.batuku.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Cria as roles na base de dados quando a aplicação arranca.
 *
 * CommandLineRunner corre automaticamente após o contexto Spring estar pronto.
 * O orElseGet garante que só insere se ainda não existir — é idempotente,
 * ou seja, podes reiniciar a app quantas vezes quiseres sem duplicar dados.
 *
 * Roles criadas:
 *   ROLE_FAN    → utilizador comum (fã)
 *   ROLE_ARTIST → artista verificado
 *   ROLE_ADMIN  → administrador da plataforma
 */
@Configuration
public class SeedRoles {

    @Bean
    @Order(1)
    CommandLineRunner seedRolesRunner(RoleRepository roleRepository) {
        return args -> {
            criarSeNaoExistir(roleRepository, "ROLE_FAN");
            criarSeNaoExistir(roleRepository, "ROLE_ARTIST");
            criarSeNaoExistir(roleRepository, "ROLE_ADMIN");
            System.out.println("Roles do Batuku verificadas/criadas.");
        };
    }

    private void criarSeNaoExistir(RoleRepository repo, String nome) {
        repo.findByName(nome).orElseGet(() -> {
            Role r = new Role();
            r.setName(nome);
            return repo.save(r);
        });
    }
}
