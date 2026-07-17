package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artist_verifications")
public class ArtistVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(name = "platform_artist_id")
    private String platformArtistId;

    @Column(name = "platform_url", length = 500)
    private String platformUrl;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public enum Platform { SPOTIFY, APPLE_MUSIC }

    public Long getId() { return id; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public String getPlatformArtistId() { return platformArtistId; }
    public void setPlatformArtistId(String platformArtistId) { this.platformArtistId = platformArtistId; }

    public String getPlatformUrl() { return platformUrl; }
    public void setPlatformUrl(String platformUrl) { this.platformUrl = platformUrl; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
