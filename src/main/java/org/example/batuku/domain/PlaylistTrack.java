package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playlist_tracks", uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "track_id"}))
public class PlaylistTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    public Long getId() { return id; }

    public Playlist getPlaylist() { return playlist; }
    public void setPlaylist(Playlist playlist) { this.playlist = playlist; }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public LocalDateTime getAddedAt() { return addedAt; }
}
