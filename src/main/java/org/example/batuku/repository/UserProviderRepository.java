package org.example.batuku.repository;

import org.example.batuku.domain.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderAndProviderId(String provider, String providerId);
    List<UserProvider> findByUserId(Long userId);
    boolean existsByUserIdAndProvider(Long userId, String provider);
}
