package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Genre;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.CreateTrackRequest;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TrackService {

    private static final long MAX_AUDIO_BYTES = 60L * 1024 * 1024; // 60 MB, mesmo limite do Publish.jsx
    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;  // 5 MB, mesmo limite do Publish.jsx

    private final TrackRepository trackRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final GenreRepository genreRepository;
    private final FileStorageService storageService;

    public TrackService(TrackRepository trackRepository,
                        ArtistProfileRepository artistProfileRepository,
                        GenreRepository genreRepository,
                        FileStorageService storageService) {
        this.trackRepository = trackRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.genreRepository = genreRepository;
        this.storageService = storageService;
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
                                  MultipartFile audio, MultipartFile cover) {
        ArtistProfile profile = getArtistProfileOrThrow(user);

        validateAudio(audio);
        if (cover != null && !cover.isEmpty()) {
            validateCover(cover);
        }

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
        track.setGenre(resolveGenre(genreName));
        track.setVisibility(Track.Visibility.PUBLIC);
        track.setPublished(true);
        return trackRepository.save(track);
    }

    /**
     * Cria uma faixa que vai pertencer a um álbum: herda o género e a capa do álbum,
     * mas tem o seu próprio ficheiro de áudio. Visibilidade de pacote para ser chamado
     * só pelo AlbumService.
     */
    Track createForAlbum(ArtistProfile profile, String title, Genre genre,
                         String albumCoverUrl, MultipartFile audio) {
        validateAudio(audio);
        String audioKey = storageService.store(audio, FileCategory.AUDIO);
        String audioUrl = storageService.resolveUrl(audioKey, FileCategory.AUDIO);

        Track track = new Track();
        track.setTitle(title);
        track.setArtistProfile(profile);
        track.setSource(Track.TrackSource.UPLOAD);
        track.setAudioUrl(audioUrl);
        track.setCoverUrl(albumCoverUrl);
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
