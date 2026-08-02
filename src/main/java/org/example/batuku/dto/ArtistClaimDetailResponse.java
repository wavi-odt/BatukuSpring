package org.example.batuku.dto;

import org.example.batuku.domain.ArtistClaimRequest;

import java.time.LocalDateTime;

public class ArtistClaimDetailResponse {

    private Long id;
    private String status;
    private Long artistProfileId;
    private String artistName;
    private Long userId;
    private String userName;
    private String userEmail;
    private String spotifyArtistId;
    private String spotifyArtistName;
    private String spotifyArtistImageUrl;
    private String selfieUrl;
    private String idDocumentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static ArtistClaimDetailResponse from(ArtistClaimRequest claim,
                                                  String selfieUrl,
                                                  String idDocumentUrl) {
        ArtistClaimDetailResponse r = new ArtistClaimDetailResponse();
        r.id = claim.getId();
        r.status = claim.getStatus().name();
        r.artistProfileId = claim.getArtistProfile().getId();
        r.artistName = claim.getArtistProfile().getName();
        r.userId = claim.getUser().getId();
        r.userName = claim.getUser().getName();
        r.userEmail = claim.getUser().getEmail();
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
    public Long getArtistProfileId() { return artistProfileId; }
    public String getArtistName() { return artistName; }
    public Long getUserId() { return userId; }
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
