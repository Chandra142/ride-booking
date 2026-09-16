package com.ridebooking.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridebooking.notification.dto.NotificationRequestDTO;
import com.ridebooking.notification.entity.Notification;
import com.ridebooking.notification.enums.NotificationStatus;
import com.ridebooking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateNotificationSuccess() throws Exception {
        NotificationRequestDTO requestDTO = NotificationRequestDTO.builder()
                .userId(1L)
                .rideId(10L)
                .paymentId(100L)
                .title("Ride Request")
                .message("Your driver is arriving shortly.")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("created successfully")))
                .andExpect(jsonPath("$.data.userId", is(1)))
                .andExpect(jsonPath("$.data.rideId", is(10)))
                .andExpect(jsonPath("$.data.paymentId", is(100)))
                .andExpect(jsonPath("$.data.title", is("Ride Request")))
                .andExpect(jsonPath("$.data.message", is("Your driver is arriving shortly.")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.notificationId", notNullValue()));
    }

    @Test
    void testCreateNotification_shouldReject_whenNotOwnNotification() throws Exception {
        NotificationRequestDTO requestDTO = NotificationRequestDTO.builder()
                .userId(2L)
                .rideId(10L)
                .title("T")
                .message("M")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetNotificationByIdSuccess() throws Exception {
        Notification notification = Notification.builder()
                .userId(2L)
                .rideId(20L)
                .title("Invoice Paid")
                .message("Receipt for ride 20.")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        mockMvc.perform(get("/api/v1/notifications/" + notification.getNotificationId())
                        .header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.notificationId", is(notification.getNotificationId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Invoice Paid")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));
    }

    @Test
    void testGetNotificationById_shouldReject_whenNotOwner() throws Exception {
        Notification notification = Notification.builder()
                .userId(2L)
                .title("Private")
                .message("M")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        notification = notificationRepository.save(notification);

        mockMvc.perform(get("/api/v1/notifications/" + notification.getNotificationId())
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetNotificationsByUserId() throws Exception {
        Notification n1 = Notification.builder()
                .userId(3L).title("T1").message("M1")
                .status(NotificationStatus.PENDING).createdAt(LocalDateTime.now())
                .build();
        Notification n2 = Notification.builder()
                .userId(3L).title("T2").message("M2")
                .status(NotificationStatus.SENT).createdAt(LocalDateTime.now())
                .build();
        Notification n3 = Notification.builder()
                .userId(4L).title("T3").message("M3")
                .status(NotificationStatus.PENDING).createdAt(LocalDateTime.now())
                .build();

        notificationRepository.saveAll(List.of(n1, n2, n3));

        mockMvc.perform(get("/api/v1/notifications/user/3")
                        .header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].userId", everyItem(is(3))));
    }

    @Test
    void testGetAllNotifications() throws Exception {
        notificationRepository.deleteAll();

        Notification n1 = Notification.builder()
                .userId(5L).title("T1").message("M1")
                .status(NotificationStatus.PENDING).createdAt(LocalDateTime.now())
                .build();
        Notification n2 = Notification.builder()
                .userId(6L).title("T2").message("M2")
                .status(NotificationStatus.PENDING).createdAt(LocalDateTime.now())
                .build();

        notificationRepository.saveAll(List.of(n1, n2));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testSendNotificationSuccess() throws Exception {
        Notification notification = Notification.builder()
                .userId(7L)
                .title("Payment Success")
                .message("Your payment was successful.")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        mockMvc.perform(put("/api/v1/notifications/" + notification.getNotificationId() + "/send")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("sent successfully")))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.sentAt", notNullValue()));

        Notification updated = notificationRepository.findById(notification.getNotificationId()).orElseThrow();
        assertEquals(NotificationStatus.SENT, updated.getStatus());
    }

    @Test
    void testSendNotificationAlreadySent() throws Exception {
        Notification notification = Notification.builder()
                .userId(8L)
                .title("Double Send")
                .message("Try to send again.")
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        mockMvc.perform(put("/api/v1/notifications/" + notification.getNotificationId() + "/send")
                        .header("X-User-Id", "8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already been sent")));
    }
}
