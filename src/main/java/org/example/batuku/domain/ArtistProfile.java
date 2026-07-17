package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "artist_profiles")
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "spotify_artist_id", nullable = true, unique = true)
    private String spotifyArtistId;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "spotify_url")
    private String spotifyUrl;

    @ElementCollection
    @CollectionTable(name = "artist_profile_genres", joinColumns = @JoinColumn(name = "artist_profile_id"))
    @Column(name = "genre")
    private List<String> genres;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    private boolean claimed = false;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpotifyArtistId() { return spotifyArtistId; }
    public void setSpotifyArtistId(String spotifyArtistId) { this.spotifyArtistId = spotifyArtistId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSpotifyUrl() { return spotifyUrl; }
    public void setSpotifyUrl(String spotifyUrl) { this.spotifyUrl = spotifyUrl; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
