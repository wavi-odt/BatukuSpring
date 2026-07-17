package org.example.batuku.dto;

import org.example.batuku.domain.Track;
import java.time.LocalDateTime;

public class TrackResponse {

    private Long id;
    private String title;
    private Long artistProfileId;
    private String artistName;
    private String source;
    private String audioUrl;
    private String spotifyUrl;
    private String coverUrl;
    private Integer durationMs;
    private long likeCount;
    private LocalDateTime createdAt;

    public static TrackResponse from(Track track, long likeCount) {
        TrackResponse r = new TrackResponse();
        r.id = track.getId();
        r.title = track.getTitle();
        r.artistProfileId = track.getArtistProfile().getId();
        r.artistName = track.getArtistProfile().getName();
        r.source = track.getSource().name();
        r.audioUrl = track.getAudioUrl();
        r.spotifyUrl = track.getSpotifyUrl();
        r.coverUrl = track.getCoverUrl();
        r.durationMs = track.getDurationMs();
        r.likeCount = likeCount;
        r.createdAt = track.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Long getArtistProfileId() { return artistProfileId; }
    public String getArtistName() { return artistName; }
    public String getSource() { return source; }
    public String getAudioUrl() { return audioUrl; }
    public String getSpotifyUrl() { return spotifyUrl; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getDurationMs() { return durationMs; }
    public long getLikeCount() { return likeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
