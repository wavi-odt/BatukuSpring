package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shares")
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharePlatform platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum SharePlatform { DISCORD, TWITTER, INSTAGRAM, COPY_LINK, OTHER }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

    public SharePlatform getPlatform() { return platform; }
    public void setPlatform(SharePlatform platform) { this.platform = platform; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
