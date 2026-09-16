package com.ridebooking.notification.sender;

import com.ridebooking.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        log.info("[MOCK] Sending notification {} to user {}: {}",
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getMessage());
    }
}
