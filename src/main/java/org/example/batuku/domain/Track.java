package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackSource source;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "spotify_track_id", unique = true)
    private String spotifyTrackId;

    @Column(name = "spotify_url", length = 300)
    private String spotifyUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    @Column(name = "is_indexed", nullable = false)
    private boolean isIndexed = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TrackSource { UPLOAD, SPOTIFY_PREVIEW }
    public enum Visibility { PUBLIC, PRIVATE, UNLISTED }

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public TrackSource getSource() { return source; }
    public void setSource(TrackSource source) { this.source = source; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getSpotifyTrackId() { return spotifyTrackId; }
    public void setSpotifyTrackId(String spotifyTrackId) { this.spotifyTrackId = spotifyTrackId; }

    public String getSpotifyUrl() { return spotifyUrl; }
    public void setSpotifyUrl(String spotifyUrl) { this.spotifyUrl = spotifyUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }

    public boolean isIndexed() { return isIndexed; }
    public void setIndexed(boolean indexed) { isIndexed = indexed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
