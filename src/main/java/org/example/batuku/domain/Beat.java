package org.example.batuku.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "beats")
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Column(nullable = false)
    private String title;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_type", length = 20)
    private LicenseType licenseType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum LicenseType { BASIC, EXCLUSIVE }

    public Long getId() { return id; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(LicenseType licenseType) { this.licenseType = licenseType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
