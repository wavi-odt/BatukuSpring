package org.example.batuku.services;

import org.example.batuku.domain.Notification;
import org.example.batuku.domain.User;
import org.example.batuku.dto.NotificationResponse;
import org.example.batuku.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User user, Notification.NotificationType type, Long referenceId, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setReferenceId(referenceId);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listar(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void marcarLida(Long notifId, Long userId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void marcarTodasLidas(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    private NotificationResponse toDto(Notification n) {
        NotificationResponse dto = new NotificationResponse();
        dto.setId(n.getId());
        dto.setType(n.getType().name());
        dto.setReferenceId(n.getReferenceId());
        dto.setMessage(n.getMessage());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
