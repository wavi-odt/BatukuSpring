package org.example.batuku.repository;

import org.example.batuku.domain.UserLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserLinkRepository extends JpaRepository<UserLink, Long> {
    List<UserLink> findByUserId(Long userId);
}
