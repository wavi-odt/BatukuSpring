package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.User;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock FollowRepository    followRepository;
    @Mock UserRepository      userRepository;
    @Mock NotificationService notificationService;
    @InjectMocks FollowService service;

    @Test
    @DisplayName("countFollowers delega no repositório")
    void countFollowers() {
        when(followRepository.countByFolloweeId(5L)).thenReturn(12L);

        assertEquals(12L, service.countFollowers(5L));
    }

    @Test
    @DisplayName("follow lança erro ao tentar seguir-se a si próprio")
    void followSiProprio() {
        User u = TestData.user(1L, User.UserRole.FAN);
        assertThrows(RuntimeException.class, () -> service.follow(u, 1L));
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("follow regista quando utilizador válido e não segue ainda")
    void followValido() {
        User follower = TestData.user(1L, User.UserRole.FAN);
        User followee = TestData.user(2L, User.UserRole.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(followee));
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(false);

        service.follow(follower, 2L);

        verify(followRepository).save(any());
    }
}
