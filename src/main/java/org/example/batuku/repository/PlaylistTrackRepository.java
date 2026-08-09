package org.example.batuku.repository;

import org.example.batuku.domain.PlaylistTrack;
import org.example.batuku.domain.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    List<PlaylistTrack> findByPlaylistIdOrderByPosition(Long playlistId);
    boolean existsByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    void deleteByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    void deleteByTrackId(Long trackId);
    long countByPlaylistId(Long playlistId);
    void deleteByPlaylistId(Long playlistId);

    @Query("SELECT DISTINCT pt.track FROM PlaylistTrack pt WHERE pt.playlist.user.id = :userId")
    List<Track> findDistinctTracksByUserId(@Param("userId") Long userId);
}
