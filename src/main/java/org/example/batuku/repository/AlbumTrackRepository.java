package org.example.batuku.repository;

import org.example.batuku.domain.AlbumTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlbumTrackRepository extends JpaRepository<AlbumTrack, Long> {
    List<AlbumTrack> findByAlbumIdOrderByPosition(Long albumId);
    List<AlbumTrack> findByTrackId(Long trackId);
    boolean existsByAlbumIdAndTrackId(Long albumId, Long trackId);
    boolean existsByTrackId(Long trackId);
    long countByAlbumId(Long albumId);
    void deleteByAlbumId(Long albumId);
    void deleteByTrackId(Long trackId);
}
