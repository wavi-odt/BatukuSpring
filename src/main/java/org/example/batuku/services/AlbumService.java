package org.example.batuku.services;

import org.example.batuku.domain.*;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.AlbumTrackRepository;
import org.example.batuku.repository.LikeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumTrackRepository albumTrackRepository;
    private final TrackService trackService;
    private final LikeRepository likeRepository;

    public AlbumService(AlbumRepository albumRepository,
                        AlbumTrackRepository albumTrackRepository,
                        TrackService trackService,
                        LikeRepository likeRepository) {
        this.albumRepository = albumRepository;
        this.albumTrackRepository = albumTrackRepository;
        this.trackService = trackService;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public ReleaseResponse createRelease(User user,
                                         String title,
                                         String genreName,
                                         String releaseType,
                                         MultipartFile cover,
                                         Map<Integer, String> trackTitles,
                                         Map<Integer, MultipartFile> trackAudios) {
        if (trackTitles == null || trackTitles.isEmpty()) {
            throw new IllegalArgumentException("Um lançamento tem de ter pelo menos uma faixa.");
        }

        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);
        Genre genre = trackService.resolveGenre(genreName);
        String coverUrl = trackService.uploadCoverIfPresent(cover);

        Album album = new Album();
        album.setTitle(title);
        album.setArtistProfile(profile);
        album.setCoverUrl(coverUrl);
        album.setAlbumType(parseAlbumType(releaseType));
        album = albumRepository.save(album);

        List<Integer> sortedIndices = trackTitles.keySet().stream().sorted().toList();
        for (int pos = 0; pos < sortedIndices.size(); pos++) {
            int idx = sortedIndices.get(pos);
            String trackTitle = trackTitles.get(idx);
            MultipartFile trackAudio = trackAudios == null ? null : trackAudios.get(idx);

            if (trackAudio == null || trackAudio.isEmpty()) {
                throw new IllegalArgumentException("Faixa #" + (pos + 1) + " não tem ficheiro de áudio.");
            }

            Track track = trackService.createForAlbum(profile, trackTitle, genre, coverUrl, trackAudio);

            AlbumTrack albumTrack = new AlbumTrack();
            albumTrack.setAlbum(album);
            albumTrack.setTrack(track);
            albumTrack.setPosition(pos + 1);
            albumTrackRepository.save(albumTrack);
        }

        return toResponse(album);
    }

    public ReleaseResponse findById(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lançamento não encontrado."));
        return toResponse(album);
    }

    public List<ReleaseResponse> listByArtist(Long artistProfileId) {
        return albumRepository.findByArtistProfileId(artistProfileId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TrackResponse> tracksOf(Long albumId) {
        return albumTrackRepository.findByAlbumIdOrderByPosition(albumId).stream()
                .map(at -> TrackResponse.from(at.getTrack(),
                        likeRepository.countByTrackId(at.getTrack().getId())))
                .toList();
    }

    private ReleaseResponse toResponse(Album album) {
        return ReleaseResponse.from(album, tracksOf(album.getId()));
    }

    private Album.AlbumType parseAlbumType(String raw) {
        if (raw == null) return Album.AlbumType.ALBUM;
        String normalized = raw.trim().toLowerCase();
        if (normalized.equals("ep")) return Album.AlbumType.EP;
        if (normalized.contains("mixtape")) return Album.AlbumType.MIXTAPE;
        if (normalized.equals("single")) return Album.AlbumType.SINGLE;
        return Album.AlbumType.ALBUM;
    }
}
