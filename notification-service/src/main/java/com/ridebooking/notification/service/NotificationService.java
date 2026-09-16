package com.ridebooking.notification.service;

import com.ridebooking.notification.dto.NotificationRequestDTO;
import com.ridebooking.notification.dto.NotificationResponseDTO;

import java.util.List;

public interface NotificationService {

    NotificationResponseDTO createNotification(NotificationRequestDTO requestDTO, String userId);

    NotificationResponseDTO getNotificationById(Long notificationId, String userId);

    List<NotificationResponseDTO> getNotificationsByUserId(Long userId);

    List<NotificationResponseDTO> getAllNotifications();

    NotificationResponseDTO sendNotification(Long notificationId, String userId);

}
