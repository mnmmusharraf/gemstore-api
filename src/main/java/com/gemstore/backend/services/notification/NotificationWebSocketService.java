package com.gemstore.backend.services.notification;

import com.gemstore.backend.dtos.notification.NotificationResponse;
import com.gemstore.backend.entities.notification.Notification;
import com.gemstore.backend.mappers.notification.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    public void sendToUser(Notification notification) {
        if (notification == null || notification.getUser() == null) {
            return;
        }

        String username = notification.getUser().getUsername();

        NotificationResponse response =
                notificationMapper.toResponse(notification);

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                response
        );

        log.info("📡 Notification sent to user {}", username);
    }

    public void broadcast(NotificationResponse notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
