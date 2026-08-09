package org.example.batuku.services;

import org.example.batuku.domain.*;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.AlbumTrackRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.LikeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumTrackRepository albumTrackRepository;
    private final TrackService trackService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public AlbumService(AlbumRepository albumRepository,
                        AlbumTrackRepository albumTrackRepository,
                        TrackService trackService,
                        LikeRepository likeRepository,
                        CommentRepository commentRepository) {
        this.albumRepository = albumRepository;
        this.albumTrackRepository = albumTrackRepository;
        this.trackService = trackService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    /** Passo 1 — cria o álbum em DRAFT com metadata + capa. Sem faixas ainda. */
    @Transactional
    public ReleaseResponse createDraft(User user, String title, String genreName,
                                       String releaseType, MultipartFile cover) {
        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);
        String coverUrl = trackService.uploadCoverIfPresent(cover);

        Album album = new Album();
        album.setTitle(title);
        album.setArtistProfile(profile);
        album.setCoverUrl(coverUrl);
        album.setAlbumType(parseAlbumType(releaseType));
        album.setStatus(Album.Status.DRAFT);
        album = albumRepository.save(album);

        return toResponse(album);
    }

    /** Passo 2 — adiciona uma faixa ao álbum DRAFT. Chamado uma vez por faixa. */
    @Transactional
    public TrackResponse addTrack(Long albumId, User user, String trackTitle, String genreName, MultipartFile audio) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Álbum não encontrado."));

        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);

        if (!album.getArtistProfile().getId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para editar este álbum.");
        }
        if (album.getStatus() != Album.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O álbum já foi publicado.");
        }

        Genre genre = trackService.resolveGenre(genreName);
        Track track = trackService.createForAlbum(profile, trackTitle, genre, album.getCoverUrl(), audio);

        int position = (int) albumTrackRepository.countByAlbumId(albumId) + 1;
        AlbumTrack albumTrack = new AlbumTrack();
        albumTrack.setAlbum(album);
        albumTrack.setTrack(track);
        albumTrack.setPosition(position);
        albumTrackRepository.save(albumTrack);

        return TrackResponse.from(track, 0);
    }

    /** Actualiza o título do lançamento. */
    @Transactional
    public ReleaseResponse updateTitle(Long albumId, User user, String title) {
        Album album = getOwnedAlbum(albumId, user);
        if (title != null && !title.isBlank()) {
            album.setTitle(title.trim());
            albumRepository.save(album);
        }
        return toResponse(album);
    }

    /** Substitui a capa do lançamento e actualiza todas as suas faixas. */
    @Transactional
    public ReleaseResponse updateCover(Long albumId, User user, MultipartFile cover) {
        Album album = getOwnedAlbum(albumId, user);
        String coverUrl = trackService.uploadCoverIfPresent(cover);
        if (coverUrl != null) {
            album.setCoverUrl(coverUrl);
            albumRepository.save(album);
            albumTrackRepository.findByAlbumIdOrderByPosition(albumId)
                    .forEach(at -> trackService.saveCoverUrl(at.getTrack(), coverUrl));
        }
        return toResponse(album);
    }

    private Album getOwnedAlbum(Long albumId, User user) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado."));
        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);
        if (!album.getArtistProfile().getId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para editar este lançamento.");
        }
        return album;
    }

    /** Passo 3 — publica o álbum (muda de DRAFT para PUBLISHED). */
    @Transactional
    public ReleaseResponse publish(Long albumId, User user) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Álbum não encontrado."));

        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);

        if (!album.getArtistProfile().getId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para publicar este álbum.");
        }
        if (album.getStatus() == Album.Status.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O álbum já está publicado.");
        }

        long trackCount = albumTrackRepository.countByAlbumId(albumId);
        if (trackCount == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Um álbum precisa de pelo menos uma faixa.");
        }

        album.setStatus(Album.Status.PUBLISHED);
        albumRepository.save(album);
        return toResponse(album);
    }

    /** Limpeza — apaga um DRAFT se o upload falhou a meio. */
    @Transactional
    public void deleteDraft(Long albumId, User user) {
        Album album = albumRepository.findById(albumId).orElse(null);
        if (album == null) return;

        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);
        if (!album.getArtistProfile().getId().equals(profile.getId())) return;
        if (album.getStatus() != Album.Status.DRAFT) return;

        albumTrackRepository.deleteByAlbumId(albumId);
        albumRepository.delete(album);
    }

    /** Elimina um lançamento e todas as suas faixas (com likes, comentários, etc.). */
    @Transactional
    public void deleteRelease(Long albumId, User user) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado."));
        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);
        if (!album.getArtistProfile().getId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para eliminar este lançamento.");
        }
        List<Track> tracks = albumTrackRepository.findByAlbumIdOrderByPosition(albumId)
                .stream().map(at -> at.getTrack()).toList();
        albumTrackRepository.deleteByAlbumId(albumId);
        commentRepository.deleteRepliesByParentAlbumId(albumId);
        commentRepository.deleteByAlbumId(albumId);
        albumRepository.delete(album);
        for (Track t : tracks) {
            trackService.purgeTrack(t);
        }
    }

    public ReleaseResponse findById(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lançamento não encontrado."));
        return toResponse(album);
    }

    public List<ReleaseResponse> listByArtist(Long artistProfileId) {
        return albumRepository.findByArtistProfileIdAndStatus(artistProfileId, Album.Status.PUBLISHED)
                .stream().map(this::toResponse).toList();
    }

    /** Todos os lançamentos do artista autenticado (PUBLISHED + DRAFT), para o dashboard. */
    public List<ReleaseResponse> listAllMine(User user) {
        ArtistProfile profile = trackService.getArtistProfileOrThrow(user);
        return albumRepository.findByArtistProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream().map(this::toResponse).toList();
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
        String n = raw.trim().toLowerCase();
        if (n.equals("ep"))              return Album.AlbumType.EP;
        if (n.contains("mixtape"))       return Album.AlbumType.MIXTAPE;
        if (n.equals("single"))          return Album.AlbumType.SINGLE;
        return Album.AlbumType.ALBUM;
    }
}
