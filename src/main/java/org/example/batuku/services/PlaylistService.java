package org.example.batuku.services;

import org.example.batuku.domain.*;
import org.example.batuku.dto.PlaylistDetailResponse;
import org.example.batuku.dto.PlaylistResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.*;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final TrackRepository trackRepository;
    private final LikeRepository likeRepository;
    private final SavedPlaylistRepository savedPlaylistRepository;
    private final FileStorageService storageService;

    public PlaylistService(PlaylistRepository playlistRepository,
                           PlaylistTrackRepository playlistTrackRepository,
                           TrackRepository trackRepository,
                           LikeRepository likeRepository,
                           SavedPlaylistRepository savedPlaylistRepository,
                           FileStorageService storageService) {
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.savedPlaylistRepository = savedPlaylistRepository;
        this.storageService = storageService;
    }

    /** Obtém ou cria a playlist de sistema "Favoritos" e sincroniza com os likes existentes. */
    @Transactional
    public Playlist getOrCreateFavorites(User user) {
        Playlist fav = playlistRepository.findByUserIdAndIsSystemGeneratedTrue(user.getId())
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setUser(user);
                    p.setTitle("Favoritos");
                    p.setSystemGenerated(true);
                    return playlistRepository.save(p);
                });

        // Backfill: garantir que todos os likes estão em playlist_tracks
        likeRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(like -> {
            Long trackId = like.getTrack().getId();
            if (!playlistTrackRepository.existsByPlaylistIdAndTrackId(fav.getId(), trackId)) {
                int pos = (int) playlistTrackRepository.countByPlaylistId(fav.getId()) + 1;
                PlaylistTrack pt = new PlaylistTrack();
                pt.setPlaylist(fav);
                pt.setTrack(like.getTrack());
                pt.setPosition(pos);
                playlistTrackRepository.save(pt);
            }
        });

        return fav;
    }

    @Transactional
    public PlaylistResponse create(User user, String name, String description, MultipartFile cover) {
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório.");

        String coverUrl = uploadCover(cover);

        Playlist playlist = new Playlist();
        playlist.setUser(user);
        playlist.setTitle(name.trim());
        playlist.setDescription(description != null ? description.trim() : null);
        playlist.setCoverUrl(coverUrl);
        playlist = playlistRepository.save(playlist);

        return PlaylistResponse.from(playlist, 0, true, false);
    }

    @Transactional
    public List<PlaylistResponse> listMine(User user) {
        getOrCreateFavorites(user);
        List<PlaylistResponse> result = new ArrayList<>();

        playlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(p -> {
            int count = (int) playlistTrackRepository.countByPlaylistId(p.getId());
            result.add(PlaylistResponse.from(p, count, true, false));
        });

        savedPlaylistRepository.findByUserIdOrderBySavedAtDesc(user.getId()).forEach(sp -> {
            Playlist p = sp.getPlaylist();
            if (p.isPublic()) {
                int count = (int) playlistTrackRepository.countByPlaylistId(p.getId());
                result.add(PlaylistResponse.from(p, count, false, true));
            }
        });

        return result;
    }

    public PlaylistDetailResponse getDetail(Long id, User user) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist não encontrada."));

        List<TrackResponse> tracks = playlistTrackRepository.findByPlaylistIdOrderByPosition(id).stream()
                .map(pt -> TrackResponse.from(pt.getTrack(), likeRepository.countByTrackId(pt.getTrack().getId())))
                .toList();

        boolean isOwner = playlist.getUser().getId().equals(user.getId());
        boolean isSaved = !isOwner && savedPlaylistRepository.existsByUserIdAndPlaylistId(user.getId(), id);
        return PlaylistDetailResponse.from(playlist, tracks, isOwner, isSaved);
    }

    @Transactional
    public PlaylistResponse update(Long id, User user, String name, String description, MultipartFile cover) {
        Playlist playlist = getOwnedPlaylist(id, user);
        if (playlist.isSystemGenerated())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A playlist Favoritos não pode ser editada.");

        if (name != null && !name.isBlank()) playlist.setTitle(name.trim());
        if (description != null) playlist.setDescription(description.trim());
        if (cover != null && !cover.isEmpty()) playlist.setCoverUrl(uploadCover(cover));
        playlist = playlistRepository.save(playlist);

        int count = (int) playlistTrackRepository.countByPlaylistId(id);
        return PlaylistResponse.from(playlist, count, true, false);
    }

    @Transactional
    public PlaylistResponse toggleVisibility(Long id, User user) {
        Playlist playlist = getOwnedPlaylist(id, user);
        if (playlist.isSystemGenerated())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A playlist Favoritos não pode ser tornada pública.");
        playlist.setPublic(!playlist.isPublic());
        playlist = playlistRepository.save(playlist);
        int count = (int) playlistTrackRepository.countByPlaylistId(id);
        return PlaylistResponse.from(playlist, count, true, false);
    }

    @Transactional
    public void savePlaylist(Long playlistId, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist não encontrada."));
        if (!playlist.isPublic())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só podes guardar playlists públicas.");
        if (playlist.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não podes guardar a tua própria playlist.");
        if (savedPlaylistRepository.existsByUserIdAndPlaylistId(user.getId(), playlistId)) return;

        SavedPlaylist sp = new SavedPlaylist();
        sp.setUser(user);
        sp.setPlaylist(playlist);
        savedPlaylistRepository.save(sp);
    }

    @Transactional
    public void unsavePlaylist(Long playlistId, User user) {
        savedPlaylistRepository.findByUserIdAndPlaylistId(user.getId(), playlistId)
                .ifPresent(savedPlaylistRepository::delete);
    }

    @Transactional
    public void delete(Long id, User user) {
        Playlist playlist = getOwnedPlaylist(id, user);
        if (playlist.isSystemGenerated())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A playlist Favoritos não pode ser eliminada.");
        playlistTrackRepository.deleteByPlaylistId(id);
        playlistRepository.delete(playlist);
    }

    @Transactional
    public void addTrack(Long playlistId, User user, Long trackId) {
        Playlist playlist = getOwnedPlaylist(playlistId, user);
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faixa não encontrada."));

        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, trackId)) return;

        int position = (int) playlistTrackRepository.countByPlaylistId(playlistId) + 1;
        PlaylistTrack pt = new PlaylistTrack();
        pt.setPlaylist(playlist);
        pt.setTrack(track);
        pt.setPosition(position);
        playlistTrackRepository.save(pt);
    }

    @Transactional
    public void removeTrack(Long playlistId, User user, Long trackId) {
        getOwnedPlaylist(playlistId, user);
        playlistTrackRepository.deleteByPlaylistIdAndTrackId(playlistId, trackId);
    }

    public List<PlaylistResponse> listPublicByUser(Long userId, User currentUser) {
        return playlistRepository
                .findByUserIdAndIsPublicTrueAndIsSystemGeneratedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(p -> {
                    int count = (int) playlistTrackRepository.countByPlaylistId(p.getId());
                    boolean isOwner = p.getUser().getId().equals(currentUser.getId());
                    boolean isSaved = !isOwner && savedPlaylistRepository.existsByUserIdAndPlaylistId(currentUser.getId(), p.getId());
                    return PlaylistResponse.from(p, count, isOwner, isSaved);
                })
                .toList();
    }

    public List<TrackResponse> libraryTracks(User user) {
        return playlistTrackRepository.findDistinctTracksByUserId(user.getId()).stream()
                .map(t -> TrackResponse.from(t, likeRepository.countByTrackId(t.getId())))
                .toList();
    }

    private Playlist getOwnedPlaylist(Long id, User user) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist não encontrada."));
        if (!playlist.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão.");
        return playlist;
    }

    private String uploadCover(MultipartFile cover) {
        if (cover == null || cover.isEmpty()) return null;
        String key = storageService.store(cover, FileCategory.PLAYLIST_COVER);
        return storageService.resolveUrl(key, FileCategory.PLAYLIST_COVER);
    }
}
