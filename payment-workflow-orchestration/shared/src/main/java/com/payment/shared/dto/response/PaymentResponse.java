package com.payment.shared.dto.response;

import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.enums.PaymentStatus;
import com.payment.shared.enums.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String paymentId;
    private PaymentStatus status;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String providerReference;
    private Provider provider;
    private boolean fxApplied;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<WorkflowStepDetail> workflowSteps;
}
