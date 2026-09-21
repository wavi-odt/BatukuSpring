package org.example.batuku.repository;

import org.example.batuku.domain.PersonalProjectTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalProjectTrackRepository extends JpaRepository<PersonalProjectTrack, Long> {
    List<PersonalProjectTrack> findByProjectIdOrderByCreatedAtAsc(Long projectId);
    Optional<PersonalProjectTrack> findByIdAndProjectId(Long id, Long projectId);
    boolean existsByIdAndProjectUserId(Long trackId, Long userId);
}
