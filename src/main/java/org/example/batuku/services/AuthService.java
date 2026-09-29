package org.example.batuku.services;

import org.example.batuku.domain.PendingRegistration;
import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.repository.LocationRepository;
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
    private final LocationRepository locationRepository;
    private final GamificationService gamificationService;
    private final EmailService emailService;

    @Value("${batuku.app.base-url}")
    private String appBaseUrl;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       PendingRegistrationRepository pendingRepository,
                       LocationRepository locationRepository,
                       GamificationService gamificationService,
                       EmailService emailService) {
        this.userRepository   = userRepository;
        this.roleRepository   = roleRepository;
        this.passwordEncoder  = passwordEncoder;
        this.pendingRepository = pendingRepository;
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
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Este email já está em uso.");
        }
        if (userRepository.existsByUsername(request.getUsername().trim().toLowerCase())) {
            throw new RuntimeException("Este username já está em uso.");
        }

        // Substitui qualquer registo pendente anterior com o mesmo email
        pendingRepository.deleteByEmail(request.getEmail().trim().toLowerCase());

        if (request.getLocation() != null && !request.getLocation().isBlank()
                && !locationRepository.existsByValue(request.getLocation())) {
            throw new RuntimeException("Localização inválida.");
        }

        boolean wantsArtist = "ARTIST".equalsIgnoreCase(request.getUserRole());

        PendingRegistration pending = new PendingRegistration();
        pending.setToken(UUID.randomUUID().toString());
        pending.setEmail(request.getEmail().trim().toLowerCase());
        pending.setUsername(request.getUsername().trim().toLowerCase());
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
     * Passo 2: valida o token, cria a conta e envia o email de boas-vindas (FAN)
     * ou de pendência (ARTIST). Apaga o registo pendente.
     */
    @Transactional
    public User confirmRegistration(String token) {
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
        String roleName = wantsArtist ? "ROLE_ARTIST" : "ROLE_FAN";
        User.UserRole userRole = wantsArtist ? User.UserRole.ARTIST : User.UserRole.FAN;

        Role springRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(roleName + " não encontrada. Verifica o SeedRoles."));

        User user = new User();
        user.setEmail(pending.getEmail());
        user.setUsername(pending.getUsername());
        user.setPassword(pending.getPassword());
        user.setName(pending.getName());
        user.setCountry(pending.getCountry());
        user.setLocation(pending.getLocation());
        user.setUserRole(userRole);
        user.setRoles(new HashSet<>(Set.of(springRole)));
        user.setEnabled(!wantsArtist);

        User saved = userRepository.save(user);
        gamificationService.inicializarPontos(saved);
        pendingRepository.delete(pending);

        if (!wantsArtist) {
            try { emailService.sendWelcomeEmail(saved); } catch (Exception ignored) {}
        }

        return saved;
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

    /**
     * Promove uma conta FAN (criada via OAuth2) para ARTIST.
     * O ArtistProfile só é criado quando o admin aceitar o claim.
     */
    @Transactional
    public void upgradeToArtist(User user) {
        if (user.getUserRole() == User.UserRole.ARTIST) return;

        Role artistRole = roleRepository.findByName("ROLE_ARTIST")
                .orElseThrow(() -> new RuntimeException("ROLE_ARTIST não encontrada."));

        user.setUserRole(User.UserRole.ARTIST);
        user.setRoles(new HashSet<>(Set.of(artistRole)));
        user.setEnabled(false);
        userRepository.save(user);
    }
}
