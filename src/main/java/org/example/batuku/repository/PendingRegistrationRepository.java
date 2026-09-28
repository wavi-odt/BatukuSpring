package org.example.batuku.repository;

import org.example.batuku.domain.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findByToken(String token);
    Optional<PendingRegistration> findByEmail(String email);
    void deleteByEmail(String email);
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
