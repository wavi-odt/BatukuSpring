package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artist_follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "artist_profile_id"}))
public class ArtistFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
