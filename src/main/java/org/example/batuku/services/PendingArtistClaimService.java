package org.example.batuku.services;

import org.example.batuku.domain.*;
import org.example.batuku.dto.PendingArtistClaimAdminResponse;
import org.example.batuku.repository.*;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PendingArtistClaimService {

    private static final Logger log = LoggerFactory.getLogger(PendingArtistClaimService.class);
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final PendingArtistClaimRepository claimRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService storageService;
    private final EmailService emailService;
    private final SpotifyClient spotifyClient;
    private final GamificationService gamificationService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DiscordAccountRepository discordAccountRepository;

    public PendingArtistClaimService(PendingArtistClaimRepository claimRepository,
                                      PendingRegistrationRepository pendingRegistrationRepository,
                                      ArtistProfileRepository artistProfileRepository,
                                      UserRepository userRepository,
                                      UserProviderRepository userProviderRepository,
                                      RoleRepository roleRepository,
                                      FileStorageService storageService,
                                      EmailService emailService,
                                      SpotifyClient spotifyClient,
                                      GamificationService gamificationService,
                                      PasswordEncoder passwordEncoder,
                                      RefreshTokenRepository refreshTokenRepository,
                                      DiscordAccountRepository discordAccountRepository) {
        this.claimRepository = claimRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
        this.roleRepository = roleRepository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.spotifyClient = spotifyClient;
        this.gamificationService = gamificationService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.discordAccountRepository = discordAccountRepository;
    }

    // ── Conversão de utilizador OAuth2 para registo pendente de artista ──

    @Transactional
    public String initFromOAuth2(User user) {
        String email = user.getEmail();

        pendingRegistrationRepository.deleteByEmail(email);

        List<UserProvider> providers = userProviderRepository.findByUserId(user.getId());
        String provider   = providers.isEmpty() ? null : providers.get(0).getProvider();
        String providerId = providers.isEmpty() ? null : providers.get(0).getProviderId();

        String claimToken = UUID.randomUUID().toString();

        PendingRegistration pending = new PendingRegistration();
        pending.setToken(UUID.randomUUID().toString());
        pending.setEmail(email);
        pending.setUsername(user.getUsername());
        pending.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        pending.setName(user.getName());
        pending.setCountry(user.getCountry());
        pending.setLocation(user.getLocation());
        pending.setUserRole("ARTIST");
        pending.setEmailVerified(true);
        pending.setClaimToken(claimToken);
        pending.setExpiresAt(LocalDateTime.now().plusDays(7));
        pending.setProvider(provider);
        pending.setProviderId(providerId);
        pending.setAvatarUrl(user.getAvatarUrl());
        pendingRegistrationRepository.save(pending);

        userProviderRepository.deleteAll(providers);
        refreshTokenRepository.deleteByUserId(user.getId());
        discordAccountRepository.findByUserId(user.getId()).ifPresent(discordAccountRepository::delete);
        claimRepository.clearReviewedBy(user.getId());
        gamificationService.removerDadosUtilizador(user);
        userRepository.delete(user);

        return claimToken;
    }

    // ── Submissão pública (sem autenticação, usa claimToken) ─────────────

    @Transactional
    public void submitClaim(String claimToken, String spotifyArtistId,
                             MultipartFile selfie, MultipartFile idDocument) {
        PendingRegistration pending = pendingRegistrationRepository.findByClaimToken(claimToken)
                .orElseThrow(() -> new IllegalArgumentException("Token de registo inválido ou expirado."));

        if (!pending.isEmailVerified()) {
            throw new IllegalStateException("O email ainda não foi verificado.");
        }
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("O link de registo expirou. Volta a registar-te.");
        }

        if (claimRepository.existsByEmailAndStatus(pending.getEmail(), PendingArtistClaim.ClaimStatus.PENDING)) {
            throw new IllegalStateException("Já existe um pedido pendente para este email.");
        }

        SpotifyClient.SpotifyArtist artist = spotifyClient.getArtist(spotifyArtistId);
        validateFile(selfie, "selfie");
        validateFile(idDocument, "documento de identificação");

        String selfieKey = storageService.store(selfie, FileCategory.SELFIE);
        String idDocKey  = storageService.store(idDocument, FileCategory.ID_DOCUMENT);

        PendingArtistClaim claim = new PendingArtistClaim();
        claim.setEmail(pending.getEmail());
        claim.setUsername(pending.getUsername());
        claim.setPasswordHash(pending.getPassword());
        claim.setName(pending.getName());
        claim.setCountry(pending.getCountry());
        claim.setLocation(pending.getLocation());
        claim.setProvider(pending.getProvider());
        claim.setProviderId(pending.getProviderId());
        claim.setAvatarUrl(pending.getAvatarUrl());
        claim.setSpotifyArtistId(spotifyArtistId);
        claim.setSpotifyArtistName(artist.name());
        claim.setSpotifyArtistImageUrl(artist.imageUrl());
        claim.setSelfieKey(selfieKey);
        claim.setIdDocumentKey(idDocKey);
        claimRepository.save(claim);

        pendingRegistrationRepository.delete(pending);

        try { emailService.sendArtistPendingEmailToAddress(claim.getEmail(), claim.getName()); }
        catch (Exception e) { log.warn("Falha ao enviar email pendente para {}: {}", claim.getEmail(), e.getMessage()); }
    }

    // ── Admin ────────────────────────────────────────────────────────────

    public List<PendingArtistClaimAdminResponse> listPending() {
        return claimRepository.findByStatusOrderByCreatedAtAsc(PendingArtistClaim.ClaimStatus.PENDING).stream()
                .map(c -> PendingArtistClaimAdminResponse.from(c,
                        storageService.resolveUrl(c.getSelfieKey(), FileCategory.SELFIE),
                        storageService.resolveUrl(c.getIdDocumentKey(), FileCategory.ID_DOCUMENT)))
                .toList();
    }

    public PendingArtistClaimAdminResponse findById(Long id) {
        PendingArtistClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + id));
        return PendingArtistClaimAdminResponse.from(claim,
                storageService.resolveUrl(claim.getSelfieKey(), FileCategory.SELFIE),
                storageService.resolveUrl(claim.getIdDocumentKey(), FileCategory.ID_DOCUMENT));
    }

    @Transactional
    public PendingArtistClaimAdminResponse verify(Long id, User admin) {
        PendingArtistClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + id));

        if (userRepository.existsByEmail(claim.getEmail())) {
            throw new IllegalStateException("Já existe uma conta com este email.");
        }
        if (userRepository.existsByUsername(claim.getUsername())) {
            throw new IllegalStateException("Já existe uma conta com este username.");
        }

        Role artistRole = roleRepository.findByName("ROLE_ARTIST")
                .orElseThrow(() -> new RuntimeException("ROLE_ARTIST não encontrada."));

        User user = new User();
        user.setEmail(claim.getEmail());
        user.setUsername(claim.getUsername());
        user.setPassword(claim.getPasswordHash());
        user.setName(claim.getName());
        user.setCountry(claim.getCountry());
        user.setLocation(claim.getLocation());
        user.setAvatarUrl(claim.getAvatarUrl());
        user.setUserRole(User.UserRole.ARTIST);
        user.setRoles(new HashSet<>(Set.of(artistRole)));
        user.setEnabled(true);
        user.setVerified(true);
        User savedUser = userRepository.save(user);

        if (claim.getProvider() != null) {
            UserProvider up = new UserProvider();
            up.setUser(savedUser);
            up.setProvider(claim.getProvider());
            up.setProviderId(claim.getProviderId());
            userProviderRepository.save(up);
        }

        gamificationService.inicializarPontos(savedUser);

        // Reutiliza perfil importado do Spotify se existir, caso contrário cria um novo
        ArtistProfile profile = artistProfileRepository
                .findBySpotifyArtistId(claim.getSpotifyArtistId())
                .filter(p -> p.getUser() == null)
                .orElseGet(() -> {
                    ArtistProfile p = new ArtistProfile();
                    p.setName(claim.getSpotifyArtistName() != null ? claim.getSpotifyArtistName() : claim.getName());
                    p.setImageUrl(claim.getSpotifyArtistImageUrl());
                    p.setSpotifyArtistId(claim.getSpotifyArtistId());
                    return p;
                });
        profile.setClaimed(true);
        profile.setUser(savedUser);
        if (claim.getSpotifyArtistName() != null) profile.setName(claim.getSpotifyArtistName());
        if (claim.getSpotifyArtistImageUrl() != null) profile.setImageUrl(claim.getSpotifyArtistImageUrl());
        ArtistProfile savedProfile = artistProfileRepository.save(profile);

        claim.setStatus(PendingArtistClaim.ClaimStatus.VERIFIED);
        claim.setReviewedBy(admin);
        claim.setReviewedAt(LocalDateTime.now());
        claimRepository.save(claim);

        try { emailService.sendClaimVerifiedEmail(savedUser, savedProfile); }
        catch (Exception e) { log.warn("Falha ao enviar email verificado para {}: {}", savedUser.getEmail(), e.getMessage()); }

        return PendingArtistClaimAdminResponse.from(claim,
                storageService.resolveUrl(claim.getSelfieKey(), FileCategory.SELFIE),
                storageService.resolveUrl(claim.getIdDocumentKey(), FileCategory.ID_DOCUMENT));
    }

    @Transactional
    public PendingArtistClaimAdminResponse markDoubtful(Long id, User admin) {
        PendingArtistClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + id));

        claim.setStatus(PendingArtistClaim.ClaimStatus.DOUBTFUL);
        claim.setReviewedBy(admin);
        claim.setReviewedAt(LocalDateTime.now());
        claimRepository.save(claim);

        try { emailService.sendArtistClaimDoubtfulToAddress(claim.getEmail(), claim.getName(), claim.getSpotifyArtistName()); }
        catch (Exception e) { log.warn("Falha ao enviar email duvidoso para {}: {}", claim.getEmail(), e.getMessage()); }

        return PendingArtistClaimAdminResponse.from(claim,
                storageService.resolveUrl(claim.getSelfieKey(), FileCategory.SELFIE),
                storageService.resolveUrl(claim.getIdDocumentKey(), FileCategory.ID_DOCUMENT));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro de " + fieldName + " é obrigatório.");
        }
        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("image/") && !ct.equals("application/pdf"))) {
            throw new IllegalArgumentException("Tipo inválido para " + fieldName + ". São aceites imagens ou PDF.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("O ficheiro de " + fieldName + " não pode exceder 10 MB.");
        }
    }
}
