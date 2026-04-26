package com.payment.orchestrator.client;

import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.LedgerTransactionResult;
import com.payment.shared.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerClient {

    private final RestTemplate restTemplate;

    @Value("${services.ledger.url}")
    private String baseUrl;

    public LedgerTransactionResult recordTransaction(Payment payment) {
        log.debug("Recording ledger transaction for paymentId={}", payment.getId());
        var request = new LedgerTransactionRequest(
                payment.getId().toString(),
                TransactionType.DEBIT,
                payment.getAmount(),
                payment.getCurrency(),
                "COMPLETED",
                payment.getProviderReference(),
                payment.getDescription()
        );
        ResponseEntity<LedgerTransactionResult> response = restTemplate.postForEntity(
                baseUrl + "/ledger/transactions", request, LedgerTransactionResult.class
        );
        return response.getBody();
    }

    private record LedgerTransactionRequest(
            String paymentId,
            TransactionType type,
            java.math.BigDecimal amount,
            String currency,
            String status,
            String providerReference,
            String description
    ) {}
}
