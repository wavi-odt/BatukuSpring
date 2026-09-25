package org.example.batuku.dto;

import org.example.batuku.domain.ArtistProfile;

public record ArtistFollowResponse(
        Long id,
        String name,
        String avatarUrl,
        String genre,
        long followerCount,
        long monthlyListeners
) {
    public static ArtistFollowResponse from(ArtistProfile p, long followerCount, long monthlyListeners) {
        String genre = (p.getGenres() != null && !p.getGenres().isEmpty()) ? p.getGenres().get(0) : null;
        String avatar = (p.isClaimed() && p.getUser() != null && p.getUser().getAvatarUrl() != null)
                ? p.getUser().getAvatarUrl()
                : p.getImageUrl();
        return new ArtistFollowResponse(p.getId(), p.getName(), avatar, genre, followerCount, monthlyListeners);
    }
}
