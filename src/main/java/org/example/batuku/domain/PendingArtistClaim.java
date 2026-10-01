package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_artist_claims")
public class PendingArtistClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados de registo (copiados de PendingRegistration no momento da submissão)
    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 100)
    private String country;

    @Column(length = 200)
    private String location;

    // Dados do claim Spotify
    @Column(name = "spotify_artist_id", length = 100)
    private String spotifyArtistId;

    @Column(name = "spotify_artist_name", length = 255)
    private String spotifyArtistName;

    @Column(name = "spotify_artist_image_url", length = 500)
    private String spotifyArtistImageUrl;

    // Preenchido apenas para registos via OAuth2
    @Column(length = 30)
    private String provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "selfie_key", nullable = false, length = 500)
    private String selfieKey;

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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSpotifyArtistId() { return spotifyArtistId; }
    public void setSpotifyArtistId(String spotifyArtistId) { this.spotifyArtistId = spotifyArtistId; }

    public String getSpotifyArtistName() { return spotifyArtistName; }
    public void setSpotifyArtistName(String spotifyArtistName) { this.spotifyArtistName = spotifyArtistName; }

    public String getSpotifyArtistImageUrl() { return spotifyArtistImageUrl; }
    public void setSpotifyArtistImageUrl(String spotifyArtistImageUrl) { this.spotifyArtistImageUrl = spotifyArtistImageUrl; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

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
