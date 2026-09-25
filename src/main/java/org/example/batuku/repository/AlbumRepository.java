package org.example.batuku.repository;

import org.example.batuku.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByArtistProfileIdAndStatus(Long artistProfileId, Album.Status status);
    List<Album> findByArtistProfileIdOrderByCreatedAtDesc(Long artistProfileId);

    @Query("SELECT DISTINCT at.album FROM AlbumTrack at WHERE at.track.genre.id = :genreId AND at.album.status = 'PUBLISHED' ORDER BY at.album.createdAt DESC")
    List<Album> findPublishedByGenreId(@Param("genreId") Long genreId);
}
