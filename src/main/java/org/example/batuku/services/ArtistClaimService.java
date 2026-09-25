package org.example.batuku.services;

import org.example.batuku.domain.ArtistClaimRequest;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistClaimDetailResponse;
import org.example.batuku.dto.ArtistClaimResponse;
import org.example.batuku.repository.ArtistClaimRequestRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtistClaimService {

    private static final long MAX_VERIFICATION_BYTES = 10L * 1024 * 1024; // 10 MB

    private final ArtistClaimRequestRepository claimRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;
    private final EmailService emailService;
    private final SpotifyClient spotifyClient;

    public ArtistClaimService(ArtistClaimRequestRepository claimRepository,
                               ArtistProfileRepository artistProfileRepository,
                               UserRepository userRepository,
                               FileStorageService storageService,
                               EmailService emailService,
                               SpotifyClient spotifyClient) {
        this.claimRepository = claimRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.spotifyClient = spotifyClient;
    }

    @Transactional
    public ArtistClaimResponse submitClaim(User user, String spotifyArtistId,
                                            MultipartFile selfie, MultipartFile idDocument) {
        ArtistProfile profile = artistProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilizador não tem perfil de artista associado."));

        if (profile.isClaimed()) {
            throw new IllegalStateException("Este perfil de artista já foi reclamado.");
        }

        if (claimRepository.existsByArtistProfileIdAndStatus(
                profile.getId(), ArtistClaimRequest.ClaimStatus.PENDING)) {
            throw new IllegalStateException(
                    "Já existe um pedido de claim pendente para este perfil. Aguarda a revisão.");
        }

        SpotifyClient.SpotifyArtist spotifyArtist = spotifyClient.getArtist(spotifyArtistId);

        validateVerificationFile(selfie, "selfie");
        validateVerificationFile(idDocument, "documento de identificação");

        String selfieKey = storageService.store(selfie, FileCategory.SELFIE);
        String idDocumentKey = storageService.store(idDocument, FileCategory.ID_DOCUMENT);

        ArtistClaimRequest claim = new ArtistClaimRequest();
        claim.setArtistProfile(profile);
        claim.setUser(user);
        claim.setSpotifyArtistId(spotifyArtistId);
        claim.setSpotifyArtistName(spotifyArtist.name());
        claim.setSpotifyArtistImageUrl(spotifyArtist.imageUrl());
        claim.setSelfieKey(selfieKey);
        claim.setIdDocumentKey(idDocumentKey);
        claim.setStatus(ArtistClaimRequest.ClaimStatus.PENDING);

        return ArtistClaimResponse.from(claimRepository.save(claim));
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

        ArtistProfile autoCreated = claim.getArtistProfile();

        // Se existe um perfil importado pelo admin com este Spotify ID, faz merge:
        // liga o utilizador a esse perfil e elimina o perfil vazio criado no registo.
        ArtistProfile imported = claim.getSpotifyArtistId() != null
                ? artistProfileRepository.findBySpotifyArtistId(claim.getSpotifyArtistId())
                        .filter(p -> p.getUser() == null)
                        .orElse(null)
                : null;

        ArtistProfile finalProfile;
        if (imported != null) {
            imported.setClaimed(true);
            imported.setUser(claim.getUser());
            if (claim.getSpotifyArtistName() != null)     imported.setName(claim.getSpotifyArtistName());
            if (claim.getSpotifyArtistImageUrl() != null) imported.setImageUrl(claim.getSpotifyArtistImageUrl());
            finalProfile = artistProfileRepository.save(imported);

            // Aponta o claim para o perfil correto antes de apagar o vazio
            claim.setArtistProfile(finalProfile);
            claimRepository.save(claim);
            artistProfileRepository.delete(autoCreated);
        } else {
            autoCreated.setClaimed(true);
            autoCreated.setUser(claim.getUser());
            if (claim.getSpotifyArtistId() != null)       autoCreated.setSpotifyArtistId(claim.getSpotifyArtistId());
            if (claim.getSpotifyArtistName() != null)     autoCreated.setName(claim.getSpotifyArtistName());
            if (claim.getSpotifyArtistImageUrl() != null) autoCreated.setImageUrl(claim.getSpotifyArtistImageUrl());
            finalProfile = autoCreated;
        }

        // Ativa a conta se estava pendente de validação
        User claimant = claim.getUser();
        if (!claimant.isEnabled()) {
            claimant.setEnabled(true);
            userRepository.save(claimant);
        }

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

        emailService.sendClaimDoubtfulEmail(claim.getUser(), claim.getArtistProfile());

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
