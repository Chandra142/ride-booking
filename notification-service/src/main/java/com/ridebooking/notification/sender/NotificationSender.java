package com.ridebooking.notification.sender;

import com.ridebooking.notification.entity.Notification;

public interface NotificationSender {
    void send(Notification notification);
}
