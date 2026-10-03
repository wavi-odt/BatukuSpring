package org.example.batuku.services;

import org.example.batuku.domain.PendingArtistClaim;
import org.example.batuku.domain.User;
import org.example.batuku.dto.AdminMetricsResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.PendingArtistClaimRepository;
import org.example.batuku.repository.UnmappedGenreRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PendingArtistClaimRepository pendingArtistClaimRepository;
    private final UnmappedGenreRepository unmappedGenreRepository;

    public AdminMetricsService(UserRepository userRepository,
                               ArtistProfileRepository artistProfileRepository,
                               PendingArtistClaimRepository pendingArtistClaimRepository,
                               UnmappedGenreRepository unmappedGenreRepository) {
        this.userRepository               = userRepository;
        this.artistProfileRepository      = artistProfileRepository;
        this.pendingArtistClaimRepository = pendingArtistClaimRepository;
        this.unmappedGenreRepository      = unmappedGenreRepository;
    }

    public AdminMetricsResponse getMetrics() {
        long totalUsers           = userRepository.countByUserRoleNot(User.UserRole.ADMIN);
        long activeArtistAccounts = userRepository.countByUserRole(User.UserRole.ARTIST);
        long importedArtists      = artistProfileRepository.count();
        long pendingClaimRequests = pendingArtistClaimRepository.countByStatus(PendingArtistClaim.ClaimStatus.PENDING);
        long unmappedGenres       = unmappedGenreRepository.count();
        return new AdminMetricsResponse(totalUsers, activeArtistAccounts, importedArtists, pendingClaimRequests, unmappedGenres);
    }
}
