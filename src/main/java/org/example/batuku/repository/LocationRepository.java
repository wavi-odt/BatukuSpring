package org.example.batuku.repository;

import org.example.batuku.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByValue(String value);
}
