package com.payment.ledger.controller;

import com.payment.ledger.service.LedgerService;
import com.payment.ledger.service.LedgerService.LedgerRequest;
import com.payment.shared.dto.response.LedgerTransactionResult;
import com.payment.shared.enums.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Immutable financial records — insert-only, no updates or deletes")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/transactions")
    @Operation(
            summary = "Record a payment transaction",
            description = "Creates an immutable Transaction record and two double-entry LedgerEntries " +
                          "(DEBIT from CLIENT_ACCOUNT, CREDIT to NOSTRO_ACCOUNT). Records can never be updated or deleted."
    )
    public ResponseEntity<LedgerTransactionResult> recordTransaction(
            @Valid @RequestBody TransactionRequest request) {
        LedgerTransactionResult result = ledgerService.recordTransaction(new LedgerRequest(
                request.paymentId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.status(),
                request.providerReference(),
                request.description()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "Get transactions for a payment",
            description = "Returns all ledger transactions recorded for the given paymentId, newest first."
    )
    public ResponseEntity<List<LedgerTransactionResult>> getTransactions(
            @RequestParam String paymentId) {
        return ResponseEntity.ok(ledgerService.getTransactionsForPayment(paymentId));
    }

    public record TransactionRequest(
            @NotBlank String paymentId,
            @NotNull TransactionType type,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            String status,
            String providerReference,
            String description
    ) {}
}
