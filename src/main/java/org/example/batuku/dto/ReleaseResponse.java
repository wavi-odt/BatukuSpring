package org.example.batuku.dto;

import org.example.batuku.domain.Album;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReleaseResponse {

    private Long id;
    private String title;
    private String albumType;
    private Long artistProfileId;
    private String artistName;
    private String coverUrl;
    private String description;
    private LocalDate releaseDate;
    private LocalDateTime createdAt;
    private List<TrackResponse> tracks;

    public static ReleaseResponse from(Album album, List<TrackResponse> tracks) {
        ReleaseResponse r = new ReleaseResponse();
        r.id = album.getId();
        r.title = album.getTitle();
        r.albumType = album.getAlbumType().name();
        r.artistProfileId = album.getArtistProfile().getId();
        r.artistName = album.getArtistProfile().getName();
        r.coverUrl = album.getCoverUrl();
        r.description = album.getDescription();
        r.releaseDate = album.getReleaseDate();
        r.createdAt = album.getCreatedAt();
        r.tracks = tracks;
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAlbumType() { return albumType; }
    public Long getArtistProfileId() { return artistProfileId; }
    public String getArtistName() { return artistName; }
    public String getCoverUrl() { return coverUrl; }
    public String getDescription() { return description; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<TrackResponse> getTracks() { return tracks; }
}
