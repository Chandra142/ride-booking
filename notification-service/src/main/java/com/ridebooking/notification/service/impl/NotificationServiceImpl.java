package com.ridebooking.notification.service.impl;

import com.ridebooking.notification.dto.NotificationRequestDTO;
import com.ridebooking.notification.dto.NotificationResponseDTO;
import com.ridebooking.notification.entity.Notification;
import com.ridebooking.notification.enums.NotificationStatus;
import com.ridebooking.notification.exception.ForbiddenException;
import com.ridebooking.notification.exception.NotificationAlreadySentException;
import com.ridebooking.notification.exception.ResourceNotFoundException;
import com.ridebooking.notification.mapper.NotificationMapper;
import com.ridebooking.notification.repository.NotificationRepository;
import com.ridebooking.notification.sender.NotificationSender;
import com.ridebooking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    @Override
    public NotificationResponseDTO createNotification(NotificationRequestDTO requestDTO, String userId) {
        Long authId = parseUserId(userId);
        if (!authId.equals(requestDTO.getUserId())) {
            throw new ForbiddenException("You can only create notifications for yourself");
        }

        Notification notification = NotificationMapper.toEntity(requestDTO);
        notification = notificationRepository.save(notification);
        return NotificationMapper.toResponseDTO(notification);
    }

    @Override
    public NotificationResponseDTO getNotificationById(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id : " + notificationId));

        Long authId = parseUserId(userId);
        if (!authId.equals(notification.getUserId())) {
            throw new ForbiddenException("You do not have access to this notification");
        }

        return NotificationMapper.toResponseDTO(notification);
    }

    @Override
    public List<NotificationResponseDTO> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .toList();
    }

    @Override
    public List<NotificationResponseDTO> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .toList();
    }

    @Override
    public NotificationResponseDTO sendNotification(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id : " + notificationId));

        Long authId = parseUserId(userId);
        if (!authId.equals(notification.getUserId())) {
            throw new ForbiddenException("You can only send your own notifications");
        }

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new NotificationAlreadySentException("Notification has already been sent.");
        }

        notificationSender.send(notification);

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);

        return NotificationMapper.toResponseDTO(notification);
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid user identity");
        }
    }
}
