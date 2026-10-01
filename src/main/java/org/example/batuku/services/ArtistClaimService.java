package org.example.batuku.services;

import org.example.batuku.domain.ArtistClaimRequest;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistClaimDetailResponse;
import org.example.batuku.dto.ArtistClaimResponse;
import org.example.batuku.repository.ArtistClaimRequestRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArtistClaimService {

    private static final long MAX_VERIFICATION_BYTES = 10L * 1024 * 1024; // 10 MB

    private final ArtistClaimRequestRepository claimRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService storageService;
    private final EmailService emailService;
    private final SpotifyClient spotifyClient;

    public ArtistClaimService(ArtistClaimRequestRepository claimRepository,
                               ArtistProfileRepository artistProfileRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               FileStorageService storageService,
                               EmailService emailService,
                               SpotifyClient spotifyClient) {
        this.claimRepository = claimRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.spotifyClient = spotifyClient;
    }

    @Transactional
    public ArtistClaimResponse submitClaim(User user, String spotifyArtistId,
                                            MultipartFile selfie, MultipartFile idDocument) {
        if (claimRepository.existsByUserIdAndStatus(user.getId(), ArtistClaimRequest.ClaimStatus.PENDING)) {
            throw new IllegalStateException(
                    "Já existe um pedido de claim pendente. Aguarda a revisão.");
        }

        artistProfileRepository.findByUserId(user.getId()).ifPresent(p -> {
            if (p.isClaimed()) throw new IllegalStateException("Este perfil de artista já foi reclamado.");
        });

        SpotifyClient.SpotifyArtist spotifyArtist = spotifyClient.getArtist(spotifyArtistId);

        validateVerificationFile(selfie, "selfie");
        validateVerificationFile(idDocument, "documento de identificação");

        String selfieKey = storageService.store(selfie, FileCategory.SELFIE);
        String idDocumentKey = storageService.store(idDocument, FileCategory.ID_DOCUMENT);

        ArtistClaimRequest claim = new ArtistClaimRequest();
        claim.setUser(user);
        claim.setSpotifyArtistId(spotifyArtistId);
        claim.setSpotifyArtistName(spotifyArtist.name());
        claim.setSpotifyArtistImageUrl(spotifyArtist.imageUrl());
        claim.setSelfieKey(selfieKey);
        claim.setIdDocumentKey(idDocumentKey);
        claim.setStatus(ArtistClaimRequest.ClaimStatus.PENDING);

        ArtistClaimResponse response = ArtistClaimResponse.from(claimRepository.save(claim));
        try { emailService.sendArtistPendingEmail(user); } catch (Exception ignored) {}
        return response;
    }

    public List<ArtistClaimResponse> findByUser(Long userId) {
        return claimRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ArtistClaimResponse::from)
                .toList();
    }

    // ── Admin operations ────────────────────────────────────────────────

    public List<ArtistClaimDetailResponse> listPending() {
        return claimRepository
                .findByStatusOrderByCreatedAtAsc(ArtistClaimRequest.ClaimStatus.PENDING)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    public ArtistClaimDetailResponse findById(Long id) {
        ArtistClaimRequest claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido de claim não encontrado: " + id));
        return toDetailResponse(claim);
    }

    @Transactional
    public ArtistClaimDetailResponse verify(Long claimId, User admin) {
        ArtistClaimRequest claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Pedido de claim não encontrado: " + claimId));

        claim.setStatus(ArtistClaimRequest.ClaimStatus.VERIFIED);
        claim.setReviewedBy(admin);
        claim.setReviewedAt(LocalDateTime.now());

        // Procura perfil importado pelo admin com este Spotify ID (sem utilizador associado)
        ArtistProfile imported = claim.getSpotifyArtistId() != null
                ? artistProfileRepository.findBySpotifyArtistId(claim.getSpotifyArtistId())
                        .filter(p -> p.getUser() == null)
                        .orElse(null)
                : null;

        // Perfil previamente auto-criado (fluxo antigo) ou null (fluxo novo)
        ArtistProfile existing = claim.getArtistProfile();

        ArtistProfile finalProfile;
        if (imported != null) {
            // Usa o perfil importado do Spotify; elimina o perfil auto-criado se existir
            imported.setClaimed(true);
            imported.setUser(claim.getUser());
            if (claim.getSpotifyArtistName() != null)     imported.setName(claim.getSpotifyArtistName());
            if (claim.getSpotifyArtistImageUrl() != null) imported.setImageUrl(claim.getSpotifyArtistImageUrl());

            if (existing != null && !existing.getId().equals(imported.getId())) {
                claim.setArtistProfile(null);
                claimRepository.save(claim);
                artistProfileRepository.delete(existing);
                artistProfileRepository.flush();
            }

            finalProfile = artistProfileRepository.save(imported);
            claim.setArtistProfile(finalProfile);
        } else if (existing != null) {
            // Fluxo antigo: atualiza o perfil auto-criado
            existing.setClaimed(true);
            existing.setUser(claim.getUser());
            if (claim.getSpotifyArtistId() != null)       existing.setSpotifyArtistId(claim.getSpotifyArtistId());
            if (claim.getSpotifyArtistName() != null)     existing.setName(claim.getSpotifyArtistName());
            if (claim.getSpotifyArtistImageUrl() != null) existing.setImageUrl(claim.getSpotifyArtistImageUrl());
            finalProfile = artistProfileRepository.save(existing);
        } else {
            // Fluxo novo: cria o perfil agora
            ArtistProfile newProfile = new ArtistProfile();
            newProfile.setName(claim.getSpotifyArtistName() != null
                    ? claim.getSpotifyArtistName() : claim.getUser().getName());
            newProfile.setImageUrl(claim.getSpotifyArtistImageUrl());
            newProfile.setSpotifyArtistId(claim.getSpotifyArtistId());
            newProfile.setClaimed(true);
            newProfile.setUser(claim.getUser());
            finalProfile = artistProfileRepository.save(newProfile);
            claim.setArtistProfile(finalProfile);
        }

        User claimant = claim.getUser();
        Role artistRole = roleRepository.findByName("ROLE_ARTIST")
                .orElseThrow(() -> new RuntimeException("ROLE_ARTIST não encontrada."));
        claimant.setUserRole(User.UserRole.ARTIST);
        claimant.setRoles(new HashSet<>(Set.of(artistRole)));
        claimant.setEnabled(true);
        userRepository.save(claimant);

        ArtistClaimDetailResponse response = toDetailResponse(claimRepository.save(claim));
        try { emailService.sendClaimVerifiedEmail(claimant, finalProfile); } catch (Exception ignored) {}
        return response;
    }

    @Transactional
    public ArtistClaimDetailResponse markDoubtful(Long claimId, User admin) {
        ArtistClaimRequest claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Pedido de claim não encontrado: " + claimId));

        claim.setStatus(ArtistClaimRequest.ClaimStatus.DOUBTFUL);
        claim.setReviewedBy(admin);
        claim.setReviewedAt(LocalDateTime.now());
        claimRepository.save(claim);

        emailService.sendClaimDoubtfulEmail(claim.getUser(), claim.getSpotifyArtistName());

        return toDetailResponse(claim);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private ArtistClaimDetailResponse toDetailResponse(ArtistClaimRequest claim) {
        String selfieUrl = storageService.resolveUrl(claim.getSelfieKey(), FileCategory.SELFIE);
        String idDocUrl = storageService.resolveUrl(claim.getIdDocumentKey(), FileCategory.ID_DOCUMENT);
        return ArtistClaimDetailResponse.from(claim, selfieUrl, idDocUrl);
    }

    private void validateVerificationFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro de " + fieldName + " é obrigatório.");
        }
        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("image/") && !ct.equals("application/pdf"))) {
            throw new IllegalArgumentException(
                    "Tipo de ficheiro inválido para " + fieldName + ". São aceites imagens (JPG, PNG) ou PDF.");
        }
        if (file.getSize() > MAX_VERIFICATION_BYTES) {
            throw new IllegalArgumentException(
                    "O ficheiro de " + fieldName + " não pode exceder 10 MB.");
        }
    }
}
