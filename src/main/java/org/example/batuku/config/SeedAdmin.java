package org.example.batuku.config;

import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.core.annotation.Order;

import java.util.Set;

/**
 * Cria o utilizador admin padrão UMA ÚNICA VEZ, na primeira vez que a app
 * arranca sem ele existir. Se o admin já existir, não faz nada — preserva
 * quaisquer alterações feitas pela aplicação (ex.: mudança de password).
 *
 * Credenciais padrão (alterar em produção via variável de ambiente ADMIN_SEED_PASSWORD):
 *   email:    batuku.suporte@gmail.com
 *   username: admin
 */
@Configuration
public class SeedAdmin {

    @Value("${batuku.admin.seed-password:*#aDMINbATUKUuP}")
    private String seedPassword;

    @Bean
    @Order(2)
    CommandLineRunner seedAdminRunner(UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail("batuku.suporte@gmail.com")) {
                return;
            }

            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN não encontrada. SeedRoles deve correr primeiro."));

            User admin = new User();
            admin.setEmail("batuku.suporte@gmail.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(seedPassword));
            admin.setName("Administrador");
            admin.setUserRole(User.UserRole.ADMIN);
            admin.setRoles(Set.of(roleAdmin));
            admin.setEnabled(true);

            userRepository.save(admin);
            System.out.println("Utilizador admin criado: batuku.suporte@gmail.com");
        };
    }
}
