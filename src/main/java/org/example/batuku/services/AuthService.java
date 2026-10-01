package org.example.batuku.services;

import org.example.batuku.domain.PendingArtistClaim;
import org.example.batuku.domain.PendingRegistration;
import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.repository.LocationRepository;
import org.example.batuku.repository.PendingArtistClaimRepository;
import org.example.batuku.repository.PendingRegistrationRepository;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PendingRegistrationRepository pendingRepository;
    private final PendingArtistClaimRepository pendingArtistClaimRepository;
    private final LocationRepository locationRepository;
    private final GamificationService gamificationService;
    private final EmailService emailService;

    @Value("${batuku.app.base-url}")
    private String appBaseUrl;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       PendingRegistrationRepository pendingRepository,
                       PendingArtistClaimRepository pendingArtistClaimRepository,
                       LocationRepository locationRepository,
                       GamificationService gamificationService,
                       EmailService emailService) {
        this.userRepository   = userRepository;
        this.roleRepository   = roleRepository;
        this.passwordEncoder  = passwordEncoder;
        this.pendingRepository = pendingRepository;
        this.pendingArtistClaimRepository = pendingArtistClaimRepository;
        this.locationRepository = locationRepository;
        this.gamificationService = gamificationService;
        this.emailService     = emailService;
    }

    /**
     * Passo 1: valida os dados, guarda o registo pendente e envia o email de verificação.
     * A conta só é criada quando o utilizador clicar no link.
     */
    @Transactional
    public void initRegistration(RegisterRequest request) {
        String email    = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Este email já está em uso.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Este username já está em uso.");
        }
        if (pendingArtistClaimRepository.existsByEmailAndStatus(email, PendingArtistClaim.ClaimStatus.PENDING)) {
            throw new RuntimeException("Já existe um pedido de registo de artista em análise para este email. Aguarda a revisão.");
        }

        // Substitui qualquer registo pendente anterior com o mesmo email
        pendingRepository.deleteByEmail(email);

        if (request.getLocation() != null && !request.getLocation().isBlank()
                && !locationRepository.existsByValue(request.getLocation())) {
            throw new RuntimeException("Localização inválida.");
        }

        boolean wantsArtist = "ARTIST".equalsIgnoreCase(request.getUserRole());

        PendingRegistration pending = new PendingRegistration();
        pending.setToken(UUID.randomUUID().toString());
        pending.setEmail(email);
        pending.setUsername(username);
        pending.setPassword(passwordEncoder.encode(request.getPassword()));
        pending.setName(request.getName().trim());
        pending.setCountry(request.getCountry());
        pending.setLocation(request.getLocation());
        pending.setUserRole(wantsArtist ? "ARTIST" : "FAN");
        pending.setExpiresAt(LocalDateTime.now().plusHours(24));

        pendingRepository.save(pending);

        String verificationUrl = appBaseUrl + "/verify-email?token=" + pending.getToken();
        emailService.sendEmailVerificationEmail(pending.getEmail(), pending.getName(), verificationUrl);
    }

    /**
     * Resultado de confirmRegistration.
     * Para FAN: user != null, claimToken == null.
     * Para ARTIST: user == null, claimToken != null.
     */
    public record ConfirmResult(User user, String claimToken) {
        public boolean isArtistPending() { return user == null; }
    }

    /**
     * Passo 2: valida o token de verificação de email.
     * - FAN: cria a conta, envia email de boas-vindas.
     * - ARTIST: marca o registo como verificado, gera claimToken (TTL 7 dias).
     *   Não cria User — o User só é criado quando o admin aprovar o claim.
     */
    @Transactional
    public ConfirmResult confirmRegistration(String token) {
        PendingRegistration pending = pendingRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link de verificação inválido ou já utilizado."));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRepository.delete(pending);
            throw new RuntimeException("O link de verificação expirou. Volta a registar-te.");
        }

        if (userRepository.existsByEmail(pending.getEmail())) {
            pendingRepository.delete(pending);
            throw new RuntimeException("Este email já está em uso.");
        }
        if (userRepository.existsByUsername(pending.getUsername())) {
            pendingRepository.delete(pending);
            throw new RuntimeException("Este username já está em uso.");
        }

        boolean wantsArtist = "ARTIST".equals(pending.getUserRole());

        if (wantsArtist) {
            String claimToken = UUID.randomUUID().toString();
            pending.setEmailVerified(true);
            pending.setClaimToken(claimToken);
            pending.setExpiresAt(LocalDateTime.now().plusDays(7));
            pendingRepository.save(pending);
            return new ConfirmResult(null, claimToken);
        }

        Role fanRole = roleRepository.findByName("ROLE_FAN")
                .orElseThrow(() -> new RuntimeException("ROLE_FAN não encontrada. Verifica o SeedRoles."));

        User user = new User();
        user.setEmail(pending.getEmail());
        user.setUsername(pending.getUsername());
        user.setPassword(pending.getPassword());
        user.setName(pending.getName());
        user.setCountry(pending.getCountry());
        user.setLocation(pending.getLocation());
        user.setUserRole(User.UserRole.FAN);
        user.setRoles(new HashSet<>(Set.of(fanRole)));
        user.setEnabled(true);

        User saved = userRepository.save(user);
        gamificationService.inicializarPontos(saved);
        pendingRepository.delete(pending);

        try { emailService.sendWelcomeEmail(saved); } catch (Exception ignored) {}

        return new ConfirmResult(saved, null);
    }

    /**
     * Gera um novo token e reenvia o email de verificação para um registo pendente.
     */
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        PendingRegistration pending = pendingRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Não existe nenhum registo pendente para este email."));

        pending.setToken(UUID.randomUUID().toString());
        pending.setExpiresAt(LocalDateTime.now().plusHours(24));
        pendingRepository.save(pending);

        String verificationUrl = appBaseUrl + "/verify-email?token=" + pending.getToken();
        emailService.sendEmailVerificationEmail(pending.getEmail(), pending.getName(), verificationUrl);
    }

    public void sendWelcomeEmail(User user) {
        emailService.sendWelcomeEmail(user);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredPendingRegistrations() {
        pendingRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

}
