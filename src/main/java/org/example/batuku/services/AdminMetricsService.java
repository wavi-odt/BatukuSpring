package org.example.batuku.services;

import org.example.batuku.domain.User;
import org.example.batuku.dto.AdminMetricsResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public AdminMetricsService(UserRepository userRepository,
                               ArtistProfileRepository artistProfileRepository) {
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    public AdminMetricsResponse getMetrics() {
        long totalUsers = userRepository.count();
        long activeArtistAccounts = userRepository.countByUserRole(User.UserRole.ARTIST);
        long importedArtists = artistProfileRepository.count();
        long pendingClaimRequests = 0; // ainda não implementado

        return new AdminMetricsResponse(totalUsers, activeArtistAccounts, importedArtists, pendingClaimRequests);
    }
}
