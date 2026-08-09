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
    private String genreName;
    private Integer durationMs;
    private long likeCount;
    private long playCount;
    private long commentCount;
    private boolean belongsToRelease;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;

    public static TrackResponse from(Track track, long likeCount) {
        return from(track, likeCount, 0L, 0L);
    }

    public static TrackResponse from(Track track, long likeCount, long playCount, long commentCount) {
        TrackResponse r = new TrackResponse();
        r.id = track.getId();
        r.title = track.getTitle();
        r.artistProfileId = track.getArtistProfile().getId();
        r.artistName = track.getArtistProfile().getName();
        r.source = track.getSource().name();
        r.audioUrl = track.getAudioUrl();
        r.spotifyUrl = track.getSpotifyUrl();
        r.coverUrl = track.getCoverUrl();
        r.genreName = track.getGenre() != null ? track.getGenre().getName() : null;
        r.durationMs = track.getDurationMs();
        r.likeCount = likeCount;
        r.playCount = playCount;
        r.commentCount = commentCount;
        r.scheduledAt = track.getScheduledAt();
        r.createdAt = track.getCreatedAt();
        if (track.isPublished()) {
            r.status = "PUBLISHED";
        } else if (track.getScheduledAt() != null) {
            r.status = "SCHEDULED";
        } else {
            r.status = "DRAFT";
        }
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
    public String getGenreName() { return genreName; }
    public long getLikeCount() { return likeCount; }
    public long getPlayCount() { return playCount; }
    public long getCommentCount() { return commentCount; }
    public boolean isBelongsToRelease() { return belongsToRelease; }
    public void setBelongsToRelease(boolean v) { this.belongsToRelease = v; }
    public String getStatus() { return status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
