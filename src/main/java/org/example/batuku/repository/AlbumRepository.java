package org.example.batuku.repository;

import org.example.batuku.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByArtistProfileIdAndStatus(Long artistProfileId, Album.Status status);
    List<Album> findByArtistProfileIdOrderByCreatedAtDesc(Long artistProfileId);
}
