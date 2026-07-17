package org.example.batuku.repository;

import org.example.batuku.domain.Beat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeatRepository extends JpaRepository<Beat, Long> {
    List<Beat> findByArtistProfileId(Long artistProfileId);
}
