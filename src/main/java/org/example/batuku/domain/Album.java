package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "albums")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "album_type", nullable = false, length = 10)
    private AlbumType albumType = AlbumType.ALBUM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlbumType { SINGLE, EP, ALBUM }

    public Long getId() { return id; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public AlbumType getAlbumType() { return albumType; }
    public void setAlbumType(AlbumType albumType) { this.albumType = albumType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
