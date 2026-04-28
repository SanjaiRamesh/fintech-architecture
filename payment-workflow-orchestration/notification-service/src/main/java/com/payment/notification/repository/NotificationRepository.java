package com.payment.notification.repository;

import com.payment.notification.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> {

    List<NotificationRecord> findByPaymentIdOrderBySentAtDesc(String paymentId);
}
