package org.example.batuku.repository;

import org.example.batuku.domain.PersonalProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonalProjectRepository extends JpaRepository<PersonalProject, Long> {
    List<PersonalProject> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PersonalProject> findByIdAndUserId(Long id, Long userId);
    Optional<PersonalProject> findByShareToken(String shareToken);

    /** Carrega o projeto e as suas faixas num único JOIN para a vista pública. */
    @Query("SELECT p FROM PersonalProject p LEFT JOIN FETCH p.tracks WHERE p.shareToken = :token")
    Optional<PersonalProject> findByShareTokenWithTracks(@Param("token") String token);
}
