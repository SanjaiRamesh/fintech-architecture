package com.payment.ledger.service;

import com.payment.ledger.entity.LedgerEntry;
import com.payment.ledger.entity.Transaction;
import com.payment.ledger.repository.LedgerEntryRepository;
import com.payment.ledger.repository.TransactionRepository;
import com.payment.shared.dto.response.LedgerTransactionResult;
import com.payment.shared.enums.TransactionStatus;
import com.payment.shared.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public LedgerTransactionResult recordTransaction(LedgerRequest request) {
        log.debug("Recording ledger transaction for paymentId={}", request.paymentId());

        TransactionStatus status = "COMPLETED".equalsIgnoreCase(request.status())
                ? TransactionStatus.COMPLETED
                : TransactionStatus.PENDING;

        Transaction transaction = Transaction.builder()
                .paymentId(request.paymentId())
                .type(request.type())
                .amount(request.amount())
                .currency(request.currency())
                .status(status)
                .providerReference(request.providerReference())
                .description(request.description())
                .build();

        transaction = transactionRepository.save(transaction);

        // Double-entry: one DEBIT from client account, one CREDIT to nostro account
        List<LedgerEntry> entries = List.of(
                LedgerEntry.builder()
                        .transactionId(transaction.getId())
                        .entryType(TransactionType.DEBIT)
                        .accountCode("CLIENT_ACCOUNT")
                        .amount(request.amount())
                        .currency(request.currency())
                        .build(),
                LedgerEntry.builder()
                        .transactionId(transaction.getId())
                        .entryType(TransactionType.CREDIT)
                        .accountCode("NOSTRO_ACCOUNT")
                        .amount(request.amount())
                        .currency(request.currency())
                        .build()
        );
        ledgerEntryRepository.saveAll(entries);

        log.info("Recorded transaction id={} paymentId={} amount={} {}",
                transaction.getId(), request.paymentId(), request.amount(), request.currency());

        return LedgerTransactionResult.builder()
                .transactionId(transaction.getId().toString())
                .paymentId(request.paymentId())
                .type(request.type())
                .amount(request.amount())
                .currency(request.currency())
                .status(status)
                .recordedAt(transaction.getCreatedAt())
                .build();
    }

    public List<LedgerTransactionResult> getTransactionsForPayment(String paymentId) {
        return transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(t -> LedgerTransactionResult.builder()
                        .transactionId(t.getId().toString())
                        .paymentId(t.getPaymentId())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .currency(t.getCurrency())
                        .status(t.getStatus())
                        .recordedAt(t.getCreatedAt())
                        .build())
                .toList();
    }

    public record LedgerRequest(
            String paymentId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            String status,
            String providerReference,
            String description
    ) {}
}
