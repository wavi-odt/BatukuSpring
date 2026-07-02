package org.example.batuku.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans de configuração geral da aplicação.
 *
 * BCryptPasswordEncoder é o standard da indústria para guardar passwords.
 * Quando o utilizador se regista, a password é transformada em hash.
 * Quando faz login, o hash é comparado — a password original nunca é guardada.
 */
@Configuration
public class AppBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
