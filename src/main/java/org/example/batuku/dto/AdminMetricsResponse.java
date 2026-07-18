package org.example.batuku.dto;

public record AdminMetricsResponse(
        long totalUsers,
        long activeArtistAccounts,
        long importedArtists,
        long pendingClaimRequests
) {}
