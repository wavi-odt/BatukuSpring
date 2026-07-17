package org.example.batuku.repository;

import org.example.batuku.domain.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    List<PlaylistTrack> findByPlaylistIdOrderByPosition(Long playlistId);
    boolean existsByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    void deleteByPlaylistIdAndTrackId(Long playlistId, Long trackId);
}
