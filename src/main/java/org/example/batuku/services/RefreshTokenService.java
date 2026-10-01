package org.example.batuku.services;

import org.example.batuku.domain.RefreshToken;
import org.example.batuku.domain.User;
import org.example.batuku.repository.RefreshTokenRepository;
import org.example.batuku.utils.JwtTokenUtil;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class RefreshTokenService {

    // ── Exceções ──────────────────────────────────────────────────────

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException() { super("Refresh token inválido."); }
    }

    public static class RefreshTokenExpiredException extends RuntimeException {
        public RefreshTokenExpiredException() { super("Refresh token expirado."); }
    }

    public static class TokenReuseDetectedException extends RuntimeException {
        public TokenReuseDetectedException() { super("Reutilização de refresh token detetada."); }
    }

    // ── Result da rotação ─────────────────────────────────────────────

    public record RotationResult(String newAccessToken, String newRefreshToken) {}

    // ── Dependências ──────────────────────────────────────────────────

    private final RefreshTokenRepository repository;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService userDetailsService;

    public RefreshTokenService(RefreshTokenRepository repository,
                                JwtTokenUtil jwtTokenUtil,
                                JwtUserDetailsService userDetailsService) {
        this.repository       = repository;
        this.jwtTokenUtil     = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    // ── Emissão ───────────────────────────────────────────────────────

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(sha256hex(raw));
        rt.setIssuedAt(LocalDateTime.now());
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        rt.setRevoked(false);
        repository.save(rt);

        return raw;
    }

    // ── Rotação ───────────────────────────────────────────────────────

    @Transactional
    public RotationResult rotate(String rawToken) {
        String hash = sha256hex(rawToken);

        RefreshToken old = repository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (old.isRevoked()) {
            // Reutilização — token já foi usado. Revoga tudo e sinaliza roubo.
            revokeAllForUser(old.getUser().getId());
            throw new TokenReuseDetectedException();
        }

        if (old.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenExpiredException();
        }

        User user = old.getUser();
        String newRaw = issue(user);

        old.setRevoked(true);
        old.setReplacedByHash(sha256hex(newRaw));
        repository.save(old);

        UserDetails ud = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtTokenUtil.generateToken(ud);

        return new RotationResult(newAccessToken, newRaw);
    }

    // ── Revogação total (logout) ──────────────────────────────────────

    @Transactional
    public void revokeAllForUser(Long userId) {
        List<RefreshToken> active = repository.findByUserIdAndRevokedFalse(userId);
        active.forEach(rt -> rt.setRevoked(true));
        repository.saveAll(active);
    }

    // ── Utilidade ─────────────────────────────────────────────────────

    private static String sha256hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
