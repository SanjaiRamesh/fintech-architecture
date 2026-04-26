package com.payment.routing.repository;

import com.payment.routing.entity.RoutingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoutingLogRepository extends JpaRepository<RoutingLog, UUID> {

    List<RoutingLog> findByPaymentIdOrderByRoutedAtDesc(String paymentId);
}
