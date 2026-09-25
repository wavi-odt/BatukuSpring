package org.example.batuku.repository;

import org.example.batuku.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    boolean existsByName(String name);
}
