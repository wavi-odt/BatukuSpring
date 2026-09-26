package org.example.batuku.services;

import org.example.batuku.domain.ArtistClaimRequest;
import org.example.batuku.domain.User;
import org.example.batuku.dto.AdminMetricsResponse;
import org.example.batuku.repository.ArtistClaimRequestRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistClaimRequestRepository claimRequestRepository;

    public AdminMetricsService(UserRepository userRepository,
                               ArtistProfileRepository artistProfileRepository,
                               ArtistClaimRequestRepository claimRequestRepository) {
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.claimRequestRepository = claimRequestRepository;
    }

    public AdminMetricsResponse getMetrics() {
        long totalUsers = userRepository.countByUserRoleNot(User.UserRole.ADMIN);
        long activeArtistAccounts = userRepository.countByUserRole(User.UserRole.ARTIST);
        long importedArtists = artistProfileRepository.count();
        long pendingClaimRequests = claimRequestRepository.countByStatus(ArtistClaimRequest.ClaimStatus.PENDING);
        return new AdminMetricsResponse(totalUsers, activeArtistAccounts, importedArtists, pendingClaimRequests);
    }
}
