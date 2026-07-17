package org.example.batuku.services;

import org.example.batuku.domain.Follow;
import org.example.batuku.domain.User;
import org.example.batuku.dto.FollowUserResponse;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void follow(User follower, Long followeeId) {
        if (follower.getId().equals(followeeId)) {
            throw new RuntimeException("Não podes seguir-te a ti próprio.");
        }
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
        if (followRepository.existsByFollowerIdAndFolloweeId(follower.getId(), followeeId)) {
            throw new RuntimeException("Já segues este utilizador.");
        }
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowee(followee);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(User follower, Long followeeId) {
        followRepository.deleteByFollowerIdAndFolloweeId(follower.getId(), followeeId);
    }

    public List<FollowUserResponse> followers(Long userId) {
        return followRepository.findByFolloweeId(userId).stream()
                .map(f -> FollowUserResponse.from(f.getFollower()))
                .toList();
    }

    public List<FollowUserResponse> following(Long userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(f -> FollowUserResponse.from(f.getFollowee()))
                .toList();
    }
}
