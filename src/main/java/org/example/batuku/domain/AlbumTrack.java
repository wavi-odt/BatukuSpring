package org.example.batuku.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "album_tracks", uniqueConstraints = @UniqueConstraint(columnNames = {"album_id", "track_id"}))
public class AlbumTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToOne(optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(nullable = false)
    private Integer position;

    public Long getId() { return id; }

    public Album getAlbum() { return album; }
    public void setAlbum(Album album) { this.album = album; }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
