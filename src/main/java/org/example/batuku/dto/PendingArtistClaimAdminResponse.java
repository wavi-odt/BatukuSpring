package org.example.batuku.dto;

import org.example.batuku.domain.PendingArtistClaim;

import java.time.LocalDateTime;

public class PendingArtistClaimAdminResponse {

    private Long id;
    private String status;
    private String artistName;
    private String userName;
    private String userEmail;
    private String spotifyArtistId;
    private String spotifyArtistName;
    private String spotifyArtistImageUrl;
    private String selfieUrl;
    private String idDocumentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static PendingArtistClaimAdminResponse from(PendingArtistClaim claim,
                                                        String selfieUrl,
                                                        String idDocumentUrl) {
        PendingArtistClaimAdminResponse r = new PendingArtistClaimAdminResponse();
        r.id = claim.getId();
        r.status = claim.getStatus().name();
        r.artistName = claim.getSpotifyArtistName() != null ? claim.getSpotifyArtistName() : claim.getName();
        r.userName = claim.getName();
        r.userEmail = claim.getEmail();
        r.spotifyArtistId = claim.getSpotifyArtistId();
        r.spotifyArtistName = claim.getSpotifyArtistName();
        r.spotifyArtistImageUrl = claim.getSpotifyArtistImageUrl();
        r.selfieUrl = selfieUrl;
        r.idDocumentUrl = idDocumentUrl;
        r.createdAt = claim.getCreatedAt();
        r.reviewedAt = claim.getReviewedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public String getArtistName() { return artistName; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getSpotifyArtistId() { return spotifyArtistId; }
    public String getSpotifyArtistName() { return spotifyArtistName; }
    public String getSpotifyArtistImageUrl() { return spotifyArtistImageUrl; }
    public String getSelfieUrl() { return selfieUrl; }
    public String getIdDocumentUrl() { return idDocumentUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}
