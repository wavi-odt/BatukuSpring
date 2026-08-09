package org.example.batuku.dto;

import org.example.batuku.domain.Playlist;

public class PlaylistResponse {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private int trackCount;
    private boolean isPublic;
    private boolean systemGenerated;
    private boolean owner;
    private boolean saved;

    public static PlaylistResponse from(Playlist p, int trackCount, boolean isOwner, boolean isSaved) {
        PlaylistResponse r = new PlaylistResponse();
        r.id = p.getId();
        r.name = p.getTitle();
        r.description = p.getDescription();
        r.coverUrl = p.getCoverUrl();
        r.trackCount = trackCount;
        r.isPublic = p.isPublic();
        r.systemGenerated = p.isSystemGenerated();
        r.owner = isOwner;
        r.saved = isSaved;
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
}
