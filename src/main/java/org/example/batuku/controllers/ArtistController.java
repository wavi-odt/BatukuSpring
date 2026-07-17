package org.example.batuku.controllers;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.dto.ArtistDetailResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ArtistController {

    private final ArtistProfileRepository artistProfileRepository;
    private final TrackRepository trackRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;

    public ArtistController(ArtistProfileRepository artistProfileRepository,
                            TrackRepository trackRepository,
                            LikeRepository likeRepository,
                            FollowRepository followRepository) {
        this.artistProfileRepository = artistProfileRepository;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.followRepository = followRepository;
    }

    @GetMapping("/{id}")
    public ArtistDetailResponse getArtist(@PathVariable Long id) {
        ArtistProfile profile = artistProfileRepository.findById(id).orElseThrow();

        List<Track> tracks = trackRepository.findByArtistProfileId(id);

        long followers = profile.getUser() != null
                ? followRepository.countByFolloweeId(profile.getUser().getId())
                : 0L;

        String genre = (profile.getGenres() != null && !profile.getGenres().isEmpty())
                ? profile.getGenres().get(0)
                : null;

        List<ArtistDetailResponse.TrackItem> trackItems = tracks.stream()
                .map(t -> new ArtistDetailResponse.TrackItem(
                        t.getId(),
                        t.getTitle(),
                        t.getCoverUrl(),
                        likeRepository.countByTrackId(t.getId()),
                        SearchController.formatDuration(t.getDurationMs())
                ))
                .toList();

        return new ArtistDetailResponse(
                profile.getId(),
                profile.getName(),
                profile.getImageUrl(),
                genre,
                null,
                null,
                followers,
                tracks.size(),
                trackItems
        );
    }
}
