package org.example.batuku.repository;

import org.example.batuku.domain.AlbumTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlbumTrackRepository extends JpaRepository<AlbumTrack, Long> {
    List<AlbumTrack> findByAlbumIdOrderByPosition(Long albumId);
    boolean existsByAlbumIdAndTrackId(Long albumId, Long trackId);
}
