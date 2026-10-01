package org.example.batuku.repository;

import org.example.batuku.domain.UserChallengeReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface UserChallengeRewardRepository extends JpaRepository<UserChallengeReward, Long> {

    boolean existsByUserIdAndChallengeIdAndSetOffset(Long userId, String challengeId, int setOffset);

    long countByUserIdAndSetOffset(Long userId, int setOffset);

    @Query("SELECT r.challengeId FROM UserChallengeReward r WHERE r.user.id = :userId AND r.setOffset = :setOffset")
    Set<String> findRewardedChallengeIds(@Param("userId") Long userId, @Param("setOffset") int setOffset);
}
