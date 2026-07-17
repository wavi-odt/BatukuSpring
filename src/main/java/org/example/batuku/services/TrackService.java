package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.CreateTrackRequest;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public TrackService(TrackRepository trackRepository,
                        ArtistProfileRepository artistProfileRepository) {
        this.trackRepository = trackRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    public Track create(User user, CreateTrackRequest request) {
        ArtistProfile profile = artistProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Só artistas com perfil conseguem publicar faixas."));

        Track track = new Track();
        track.setTitle(request.getTitle());
        track.setAudioUrl(request.getAudioUrl());
        track.setSource(Track.TrackSource.UPLOAD);
        track.setArtistProfile(profile);
        return trackRepository.save(track);
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
}
