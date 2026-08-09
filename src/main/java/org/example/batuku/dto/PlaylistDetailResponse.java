package org.example.batuku.dto;

import org.example.batuku.domain.Playlist;
import java.util.List;

public class PlaylistDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private int trackCount;
    private boolean isPublic;
    private boolean systemGenerated;
    private boolean owner;
    private boolean saved;
    private List<TrackResponse> tracks;

    public static PlaylistDetailResponse from(Playlist p, List<TrackResponse> tracks, boolean isOwner, boolean isSaved) {
        PlaylistDetailResponse r = new PlaylistDetailResponse();
        r.id = p.getId();
        r.name = p.getTitle();
        r.description = p.getDescription();
        r.coverUrl = p.getCoverUrl();
        r.trackCount = tracks.size();
        r.isPublic = p.isPublic();
        r.systemGenerated = p.isSystemGenerated();
        r.owner = isOwner;
        r.saved = isSaved;
        r.tracks = tracks;
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public int getTrackCount() { return trackCount; }
    public boolean getIsPublic() { return isPublic; }
    public boolean isSystemGenerated() { return systemGenerated; }
    public boolean isOwner() { return owner; }
    public boolean isSaved() { return saved; }
    public List<TrackResponse> getTracks() { return tracks; }
}
