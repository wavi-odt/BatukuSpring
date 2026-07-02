package org.example.batuku.config;

import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.core.annotation.Order;

import java.util.Set;

/**
 * Cria o utilizador admin padrão se ainda não existir.
 *
 * Credenciais padrão (alterar em produção via variáveis de ambiente):
 *   email:    admin@batuku.com
 *   username: admin
 *   password: admin1234
 */
@Configuration
public class SeedAdmin {

    @Bean
    @Order(2)
    CommandLineRunner seedAdminRunner(UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail("admin@batuku.com")) {
                return;
            }

            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN não encontrada. SeedRoles deve correr primeiro."));

            User admin = new User();
            admin.setEmail("admin@batuku.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setName("Administrador");
            admin.setUserRole(User.UserRole.ADMIN);
            admin.setRoles(Set.of(roleAdmin));
            admin.setEnabled(true);

            userRepository.save(admin);
            System.out.println("Utilizador admin criado: admin@batuku.com / admin1234");
        };
    }
}
