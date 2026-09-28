package org.example.batuku.dto;

import org.example.batuku.domain.ArtistClaimRequest;

import java.time.LocalDateTime;

public class ArtistClaimResponse {

    private Long id;
    private String status;
    private Long artistProfileId;
    private String artistName;
    private LocalDateTime createdAt;

    public static ArtistClaimResponse from(ArtistClaimRequest claim) {
        ArtistClaimResponse r = new ArtistClaimResponse();
        r.id = claim.getId();
        r.status = claim.getStatus().name();
        r.artistProfileId = claim.getArtistProfile() != null ? claim.getArtistProfile().getId() : null;
        r.artistName = claim.getArtistProfile() != null
                ? claim.getArtistProfile().getName() : claim.getSpotifyArtistName();
        r.createdAt = claim.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public Long getArtistProfileId() { return artistProfileId; }
    public String getArtistName() { return artistName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
