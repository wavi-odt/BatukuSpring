package org.example.batuku.repository;

import org.example.batuku.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de User.
 *
 * O Spring Data JPA implementa automaticamente estes métodos
 * a partir dos nomes, não precisas escrever SQL.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // Login flexível: aceita email ou username num único query
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@org.springframework.data.repository.query.Param("identifier") String identifier);

    List<User> findTop5ByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) ORDER BY u.createdAt DESC")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    List<User> findTop50ByOrderByCreatedAtDesc();

    long countByUserRole(User.UserRole userRole);
    long countByUserRoleNot(User.UserRole userRole);

}
