package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.AlbumTrack;
import org.example.batuku.domain.Genre;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.CreateTrackRequest;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.AlbumTrackRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlaylistTrackRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.ShareRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.config.StorageProperties;
import org.example.batuku.storage.AudioDurationUtil;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackService.class);

    private static final long MAX_AUDIO_BYTES = 300L * 1024 * 1024; // 300 MB, mesmo limite do Publish.jsx
    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;  // 5 MB, mesmo limite do Publish.jsx

    private final TrackRepository trackRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final GenreRepository genreRepository;
    private final FileStorageService storageService;
    private final StorageProperties storageProperties;
    private final AlbumTrackRepository albumTrackRepository;
    private final AlbumRepository albumRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final PlayRepository playRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final ShareRepository shareRepository;

    public TrackService(TrackRepository trackRepository,
                        ArtistProfileRepository artistProfileRepository,
                        GenreRepository genreRepository,
                        FileStorageService storageService,
                        StorageProperties storageProperties,
                        AlbumTrackRepository albumTrackRepository,
                        AlbumRepository albumRepository,
                        LikeRepository likeRepository,
                        CommentRepository commentRepository,
                        PlayRepository playRepository,
                        PlaylistTrackRepository playlistTrackRepository,
                        ShareRepository shareRepository) {
        this.trackRepository = trackRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.genreRepository = genreRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.albumTrackRepository = albumTrackRepository;
        this.albumRepository = albumRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.playRepository = playRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.shareRepository = shareRepository;
    }

    /**
     * Fluxo antigo (JSON, audioUrl ja pronto). Mantido para compatibilidade,
     * mas o Publish.jsx ja usa createFromUpload() abaixo.
     */
    public Track create(User user, CreateTrackRequest request) {
        ArtistProfile profile = getArtistProfileOrThrow(user);

        Track track = new Track();
        track.setTitle(request.getTitle());
        track.setAudioUrl(request.getAudioUrl());
        track.setSource(Track.TrackSource.UPLOAD);
        track.setArtistProfile(profile);
        return trackRepository.save(track);
    }

    /**
     * Fluxo real usado pelo Publish.jsx: titulo + genero (texto) + ficheiro de
     * audio + capa opcional, tudo numa so chamada multipart. Faz a validacao,
     * o upload para o storage (local ou R2) e cria a Track ja publicada.
     */
    public Track createFromUpload(User user, String title, String genreName,
                                  MultipartFile audio, MultipartFile cover,
                                  LocalDateTime scheduledAt) {
        ArtistProfile profile = getArtistProfileOrThrow(user);

        validateAudio(audio);
        if (cover != null && !cover.isEmpty()) {
            validateCover(cover);
        }

        Integer durationMs = extractDurationMs(audio);

        String audioKey = storageService.store(audio, FileCategory.AUDIO);
        String audioUrl = storageService.resolveUrl(audioKey, FileCategory.AUDIO);

        String coverUrl = null;
        if (cover != null && !cover.isEmpty()) {
            String coverKey = storageService.store(cover, FileCategory.COVER);
            coverUrl = storageService.resolveUrl(coverKey, FileCategory.COVER);
        }

        Track track = new Track();
        track.setTitle(title);
        track.setArtistProfile(profile);
        track.setSource(Track.TrackSource.UPLOAD);
        track.setAudioUrl(audioUrl);
        track.setCoverUrl(coverUrl);
        track.setDurationMs(durationMs);
        track.setGenre(resolveGenre(genreName));
        track.setVisibility(Track.Visibility.PUBLIC);

        if (scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now())) {
            track.setScheduledAt(scheduledAt);
            track.setPublished(false);
        } else {
            track.setPublished(true);
        }

        return trackRepository.save(track);
    }

    @Transactional
    public void delete(Long trackId, User user) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Faixa não encontrada."));
        ArtistProfile profile = getArtistProfileOrThrow(user);
        if (!track.getArtistProfile().getId().equals(profile.getId())) {
            throw new SecurityException("Não tens permissão para eliminar esta faixa.");
        }
        List<AlbumTrack> albumTracks = albumTrackRepository.findByTrackId(trackId);
        deleteDependencies(trackId);
        trackRepository.delete(track);
        for (AlbumTrack at : albumTracks) {
            if (albumTrackRepository.countByAlbumId(at.getAlbum().getId()) == 0) {
                albumRepository.delete(at.getAlbum());
            }
        }
    }

    /** Elimina uma faixa e todas as suas dependências sem verificar propriedade. Chamado pelo AlbumService. */
    @Transactional
    public void purgeTrack(Track track) {
        deleteDependencies(track.getId());
        trackRepository.delete(track);
    }

    private void deleteDependencies(Long trackId) {
        likeRepository.deleteByTrackId(trackId);
        commentRepository.deleteRepliesByParentTrackId(trackId);
        commentRepository.deleteByTrackId(trackId);
        playRepository.deleteByTrackId(trackId);
        playlistTrackRepository.deleteByTrackId(trackId);
        shareRepository.deleteByTrackId(trackId);
        albumTrackRepository.deleteByTrackId(trackId);
    }

    @Scheduled(fixedDelay = 60_000)
    public void publishDueTracks() {
        List<Track> due = trackRepository.findByScheduledAtBeforeAndIsPublishedFalse(LocalDateTime.now());
        for (Track t : due) {
            t.setPublished(true);
            trackRepository.save(t);
            log.info("Auto-publicada faixa agendada: id={} title={}", t.getId(), t.getTitle());
        }
    }

    /**
     * Cria uma faixa que vai pertencer a um álbum: herda o género e a capa do álbum,
     * mas tem o seu próprio ficheiro de áudio. Visibilidade de pacote para ser chamado
     * só pelo AlbumService.
     */
    Track createForAlbum(ArtistProfile profile, String title, Genre genre,
                         String albumCoverUrl, MultipartFile audio) {
        validateAudio(audio);
        Integer durationMs = extractDurationMs(audio);
        String audioKey = storageService.store(audio, FileCategory.AUDIO);
        String audioUrl = storageService.resolveUrl(audioKey, FileCategory.AUDIO);

        Track track = new Track();
        track.setTitle(title);
        track.setArtistProfile(profile);
        track.setSource(Track.TrackSource.UPLOAD);
        track.setAudioUrl(audioUrl);
        track.setCoverUrl(albumCoverUrl);
        track.setDurationMs(durationMs);
        track.setGenre(genre);
        track.setVisibility(Track.Visibility.PUBLIC);
        track.setPublished(true);
        return trackRepository.save(track);
    }

    /** Valida e faz upload da capa, se estiver presente; devolve a URL ou null. */
    String uploadCoverIfPresent(MultipartFile cover) {
        if (cover == null || cover.isEmpty()) return null;
        validateCover(cover);
        String key = storageService.store(cover, FileCategory.COVER);
        return storageService.resolveUrl(key, FileCategory.COVER);
    }

    public Track findById(Long id) {
        return trackRepository.findById(id).orElseThrow();
    }

    public List<Track> listAll() {
        return trackRepository.findAll();
    }

    public List<Track> listByArtist(Long artistProfileId) {
        return trackRepository.findByArtistProfileId(artistProfileId);
    }

    @Transactional
    public Track updateCover(Long id, User user, MultipartFile cover) {
        Track track = trackRepository.findById(id).orElseThrow();
        ArtistProfile profile = getArtistProfileOrThrow(user);
        if (!track.getArtistProfile().getId().equals(profile.getId())) {
            throw new SecurityException("Sem permissão para editar esta faixa.");
        }
        if (albumTrackRepository.existsByTrackId(id)) {
            throw new IllegalArgumentException("A capa desta faixa é gerida pelo lançamento ao qual pertence.");
        }
        String coverUrl = uploadCoverIfPresent(cover);
        if (coverUrl != null) {
            track.setCoverUrl(coverUrl);
            trackRepository.save(track);
        }
        return track;
    }

    /** Actualiza só o coverUrl de uma track — chamado pelo AlbumService em cascade. */
    void saveCoverUrl(Track track, String coverUrl) {
        track.setCoverUrl(coverUrl);
        trackRepository.save(track);
    }

    @Transactional
    public Track update(Long id, String title, String genreName, User user) {
        Track track = trackRepository.findById(id).orElseThrow();
        ArtistProfile profile = getArtistProfileOrThrow(user);
        if (!track.getArtistProfile().getId().equals(profile.getId())) {
            throw new SecurityException("Sem permissão para editar esta faixa.");
        }
        if (title != null && !title.isBlank()) track.setTitle(title.trim());
        if (genreName != null) track.setGenre(resolveGenre(genreName.isBlank() ? null : genreName));
        return trackRepository.save(track);
    }

    // Helpers, reutilizados tambem pelo AlbumService

    ArtistProfile getArtistProfileOrThrow(User user) {
        return artistProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("So artistas com perfil conseguem publicar faixas."));
    }

    void validateAudio(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("E obrigatorio escolher um ficheiro de audio.");
        }
        String contentType = audio.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("Tipo de ficheiro invalido. So sao aceites ficheiros de audio.");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("O ficheiro de audio nao pode exceder 60 MB.");
        }
    }

    void validateCover(MultipartFile cover) {
        String contentType = cover.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Tipo de ficheiro invalido para a capa. So sao aceites imagens.");
        }
        if (cover.getSize() > MAX_COVER_BYTES) {
            throw new IllegalArgumentException("A capa nao pode exceder 5 MB.");
        }
    }

    /**
     * Extrai a duração em milissegundos de um MultipartFile de áudio.
     * Usa getBytes() para não consumir o stream (storageService.store precisa dele depois).
     */
    Integer extractDurationMs(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String ct = file.getContentType();
        String ext = (ct != null && (ct.contains("mpeg") || ct.contains("mp3"))) ? ".mp3" : ".wav";
        try {
            byte[] bytes = file.getBytes();
            Path tmp = Files.createTempFile("batuku-dur-", ext);
            try {
                Files.write(tmp, bytes);
                return AudioDurationUtil.extractMs(tmp);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (Exception e) {
            log.warn("Não foi possível extrair duração do áudio: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Percorre todas as tracks UPLOAD sem durationMs e tenta extraí-lo dos
     * ficheiros locais. Usado uma única vez para corrigir registos antigos.
     */
    public Map<String, Object> backfillDurations() {
        String publicBaseUrl = storageProperties.getLocal().getPublicBaseUrl();
        Path base = Path.of(storageProperties.getLocal().getBaseDir()).toAbsolutePath();

        List<Track> tracks = trackRepository.findBySourceAndDurationMsIsNull(Track.TrackSource.UPLOAD);
        int updated = 0, failed = 0;

        for (Track track : tracks) {
            String url = track.getAudioUrl();
            if (url == null || !url.startsWith(publicBaseUrl + "/")) { failed++; continue; }

            String key = url.substring(publicBaseUrl.length() + 1);
            Path file = base.resolve(key);
            Integer ms = AudioDurationUtil.extractMs(file);

            if (ms != null) {
                track.setDurationMs(ms);
                trackRepository.save(track);
                updated++;
                log.info("backfill: track {} → {}ms", track.getId(), ms);
            } else {
                failed++;
                log.warn("backfill: track {} sem duração", track.getId());
            }
        }
        return Map.of("total", tracks.size(), "updated", updated, "failed", failed);
    }

    /**
     * Procura o genero pelo nome (ignorando maiusculas/minusculas); se nao
     * existir ainda na tabela genres, cria-o. Isto cobre a lista fixa do
     * Publish.jsx (Funana, Batuku, Morna, Coladeira, Kizomba, Afrobeat,
     * Hip-Hop, Outro) sem seres obrigado a pre-popular a tabela.
     */
    Genre resolveGenre(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();
        return genreRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setName(trimmed);
                    return genreRepository.save(genre);
                });
    }
}
