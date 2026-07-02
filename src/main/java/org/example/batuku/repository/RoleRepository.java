package org.example.batuku.repository;

import org.example.batuku.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório de Role.
 * Usado principalmente no seed inicial e no registo de utilizadores.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
