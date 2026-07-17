package org.example.batuku.repository;

import org.example.batuku.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserId(Long userId);
}
