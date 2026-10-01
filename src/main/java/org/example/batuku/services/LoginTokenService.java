package org.example.batuku.services;

import org.example.batuku.domain.LoginToken;
import org.example.batuku.domain.User;
import org.example.batuku.repository.LoginTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoginTokenService {

    private final LoginTokenRepository loginTokenRepository;

    public LoginTokenService(LoginTokenRepository loginTokenRepository) {
        this.loginTokenRepository = loginTokenRepository;
    }

    @Transactional
    public String generate(User user, String redirectPath) {
        String token = UUID.randomUUID().toString();
        loginTokenRepository.save(new LoginToken(token, user, LocalDateTime.now().plusHours(24), redirectPath));
        return token;
    }

    @Transactional
    public LoginToken consume(String token) {
        LoginToken lt = loginTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link inválido ou já utilizado."));

        if (lt.isUsed()) throw new RuntimeException("Este link já foi utilizado.");
        if (lt.getExpiresAt().isBefore(LocalDateTime.now())) throw new RuntimeException("Este link expirou.");

        lt.setUsed(true);
        return loginTokenRepository.save(lt);
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purge() {
        loginTokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
    }
}
