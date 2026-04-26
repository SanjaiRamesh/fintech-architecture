package com.payment.shared.dto.response;

import com.payment.shared.enums.TransactionStatus;
import com.payment.shared.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransactionResult {

    private String transactionId;
    private String paymentId;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private Instant recordedAt;
}
