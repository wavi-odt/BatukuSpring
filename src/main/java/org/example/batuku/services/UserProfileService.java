package org.example.batuku.services;

import org.example.batuku.domain.User;
import org.example.batuku.dto.UserDetailResponse;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.BadgeRepository;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.LocationRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.UserBadgeRepository;
import org.example.batuku.repository.UserPointsRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private final UserRepository           userRepository;
    private final FollowRepository         followRepository;
    private final ArtistFollowRepository   artistFollowRepository;
    private final ArtistProfileRepository  artistProfileRepository;
    private final LocationRepository       locationRepository;
    private final PlayRepository           playRepository;
    private final UserPointsRepository     userPointsRepository;
    private final UserBadgeRepository      userBadgeRepository;
    private final BadgeRepository          badgeRepository;
    private final PasswordEncoder          passwordEncoder;
    private final FileStorageService       storageService;

    public UserProfileService(UserRepository userRepository,
                              FollowRepository followRepository,
                              ArtistFollowRepository artistFollowRepository,
                              ArtistProfileRepository artistProfileRepository,
                              LocationRepository locationRepository,
                              PlayRepository playRepository,
                              UserPointsRepository userPointsRepository,
                              UserBadgeRepository userBadgeRepository,
                              BadgeRepository badgeRepository,
                              PasswordEncoder passwordEncoder,
                              FileStorageService storageService) {
        this.userRepository          = userRepository;
        this.followRepository        = followRepository;
        this.artistFollowRepository  = artistFollowRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.locationRepository      = locationRepository;
        this.playRepository          = playRepository;
        this.userPointsRepository    = userPointsRepository;
        this.userBadgeRepository     = userBadgeRepository;
        this.badgeRepository         = badgeRepository;
        this.passwordEncoder         = passwordEncoder;
        this.storageService          = storageService;
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long id, User viewer) {
        User user = userRepository.findById(id).orElseThrow();
        Long artistProfileId = artistProfileRepository.findByUserId(id)
                .map(a -> a.getId()).orElse(null);

        long followers = artistProfileId != null
                ? artistFollowRepository.countByArtistProfileId(artistProfileId)
                : followRepository.countByFolloweeId(id);
        long following = followRepository.countByFollowerId(id)
                       + artistFollowRepository.countByFollowerId(id);

        boolean isFollowing = false;
        if (viewer != null) {
            isFollowing = artistProfileId != null
                    ? artistFollowRepository.existsByFollowerIdAndArtistProfileId(viewer.getId(), artistProfileId)
                    : followRepository.existsByFollowerIdAndFolloweeId(viewer.getId(), id);
        }

        int  points = userPointsRepository.findByUserId(id).map(up -> up.getTotalPoints()).orElse(0);
        long rank   = userPointsRepository.findByUserId(id).isPresent()
                ? userPointsRepository.findRankByUserId(id) : 0L;

        var earnedBadges = userBadgeRepository.findByUserId(id);
        var earnedIds    = earnedBadges.stream()
                .map(ub -> ub.getBadge().getId())
                .collect(Collectors.toSet());
        var earnedAtMap  = earnedBadges.stream()
                .collect(Collectors.toMap(
                        ub -> ub.getBadge().getId(),
                        ub -> ub.getEarnedAt() != null ? ub.getEarnedAt().toString() : "",
                        (existing, replacement) -> existing));

        var badges = badgeRepository.findAll().stream()
                .map(b -> new UserDetailResponse.BadgeResult(
                        b.getId(), b.getName(), b.getDescription(), b.getIconUrl(),
                        b.getPointsRequired(), earnedIds.contains(b.getId()),
                        earnedAtMap.getOrDefault(b.getId(), null)))
                .toList();

        return new UserDetailResponse(
                user.getId(),
                user.getName(),
                "@" + user.getUsername(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getLocation(),
                followers,
                following,
                isFollowing,
                0,
                artistProfileId,
                points,
                rank,
                earnedIds.size(),
                badges
        );
    }

    @Transactional
    public UserResponse updateNameUsername(User user, String name, String username) {
        String newUsername = username.trim();
        if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            throw new IllegalStateException("Este username já está em uso.");
        }
        user.setName(name.trim());
        user.setUsername(newUsername);
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateLocation(User user, String location) {
        if (!location.isEmpty() && !locationRepository.existsByValue(location)) {
            throw new IllegalArgumentException("Localização inválida.");
        }
        user.setLocation(location.isEmpty() ? null : location);
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void setPassword(User user, String newPassword) {
        if (user.getPassword() != null && !user.getPassword().startsWith("{OAUTH}")) {
            throw new IllegalStateException("Já tens uma palavra-passe definida. Usa a opção de alterar.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("A palavra-passe deve ter pelo menos 8 caracteres.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user.getPassword() == null || user.getPassword().startsWith("{OAUTH}") ||
                !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Palavra-passe atual incorreta.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public String setMarketplaceRole(User user, String role) {
        if (user.getMarketplaceRole() != null) {
            throw new IllegalStateException("Papel no marketplace já definido.");
        }
        String normalized = role.toUpperCase();
        if (!normalized.equals("FAN") && !normalized.equals("PRODUCER")) {
            throw new IllegalArgumentException("Papel inválido.");
        }
        user.setMarketplaceRole(normalized);
        userRepository.save(user);
        return normalized;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentlyPlayed(Long userId) {
        userRepository.findById(userId).orElseThrow();
        return buildRecentlyPlayed(playRepository.findRecentlyPlayedByUser(userId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopGenres(Long userId) {
        userRepository.findById(userId).orElseThrow();
        return buildTopGenres(playRepository.findTopGenresByUser(userId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyRecentlyPlayed(User user) {
        return buildRecentlyPlayed(playRepository.findRecentlyPlayedByUser(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyTopGenres(User user) {
        return buildTopGenres(playRepository.findTopGenresByUser(user.getId()));
    }

    @Transactional
    public UserResponse updateAvatar(User user, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Tipo de ficheiro inválido. Só são aceites imagens.");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("O ficheiro de avatar não pode exceder 5 MB.");
        }
        String key = storageService.store(file, FileCategory.AVATAR);
        String url = storageService.resolveUrl(key, FileCategory.AVATAR);
        user.setAvatarUrl(url);
        return UserResponse.from(userRepository.save(user));
    }

    public Map<String, String> uploadAudio(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("Tipo de ficheiro inválido. Só são aceites ficheiros de áudio.");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new IllegalArgumentException("O ficheiro de áudio não pode exceder 20 MB.");
        }
        String key = storageService.store(file, FileCategory.AUDIO);
        String url = storageService.resolveUrl(key, FileCategory.AUDIO);
        return Map.of("key", key, "url", url);
    }

    private List<Map<String, Object>> buildRecentlyPlayed(List<Object[]> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("trackId",    ((Number) row[0]).longValue());
            item.put("title",      row[1]);
            item.put("coverUrl",   row[2]);
            item.put("artistName", row[3]);
            item.put("artistId",   ((Number) row[4]).longValue());
            item.put("audioUrl",   row[5]);
            item.put("source",     row[6]);
            item.put("spotifyId",  row[7]);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildTopGenres(List<Object[]> rows) {
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String name  = (String) row[0];
            long   plays = ((Number) row[1]).longValue();
            int    pct   = total > 0 ? (int) Math.round(plays * 100.0 / total) : 0;
            result.add(Map.of("name", name, "plays", plays, "pct", pct));
        }
        return result;
    }
}
