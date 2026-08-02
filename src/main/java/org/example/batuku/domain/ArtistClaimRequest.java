package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artist_claim_requests")
public class ArtistClaimRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "spotify_artist_id", length = 100)
    private String spotifyArtistId;

    @Column(name = "spotify_artist_name", length = 255)
    private String spotifyArtistName;

    @Column(name = "spotify_artist_image_url", length = 500)
    private String spotifyArtistImageUrl;

    /** Chave de objeto no storage (não URL expirada) — presigned URL gerada em cada pedido. */
    @Column(name = "selfie_key", nullable = false, length = 500)
    private String selfieKey;

    /** Chave de objeto no storage (não URL expirada) — presigned URL gerada em cada pedido. */
    @Column(name = "id_document_key", nullable = false, length = 500)
    private String idDocumentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ClaimStatus { PENDING, VERIFIED, DOUBTFUL }

    public Long getId() { return id; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSpotifyArtistId() { return spotifyArtistId; }
    public void setSpotifyArtistId(String spotifyArtistId) { this.spotifyArtistId = spotifyArtistId; }

    public String getSpotifyArtistName() { return spotifyArtistName; }
    public void setSpotifyArtistName(String spotifyArtistName) { this.spotifyArtistName = spotifyArtistName; }

    public String getSpotifyArtistImageUrl() { return spotifyArtistImageUrl; }
    public void setSpotifyArtistImageUrl(String spotifyArtistImageUrl) { this.spotifyArtistImageUrl = spotifyArtistImageUrl; }

    public String getSelfieKey() { return selfieKey; }
    public void setSelfieKey(String selfieKey) { this.selfieKey = selfieKey; }

    public String getIdDocumentKey() { return idDocumentKey; }
    public void setIdDocumentKey(String idDocumentKey) { this.idDocumentKey = idDocumentKey; }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
