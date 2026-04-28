package com.payment.notification.controller;

import com.payment.notification.entity.NotificationRecord;
import com.payment.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Query dispatched payment notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "Get notifications for a payment",
            description = "Returns all notification records (webhook, email, SMS) dispatched for the given paymentId. " +
                          "Useful for debugging — if a client says they didn't receive a webhook, check here first."
    )
    public ResponseEntity<List<NotificationRecord>> getNotifications(@RequestParam String paymentId) {
        return ResponseEntity.ok(notificationService.getNotificationsForPayment(paymentId));
    }
}
