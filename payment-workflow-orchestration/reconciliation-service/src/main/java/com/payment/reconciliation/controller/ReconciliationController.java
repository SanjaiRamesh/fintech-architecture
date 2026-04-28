package com.payment.reconciliation.controller;

import com.payment.reconciliation.entity.ReconciliationEntry;
import com.payment.reconciliation.entity.ReconciliationJob;
import com.payment.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Reconciliation", description = "Payment vs provider settlement reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    @Operation(
            summary = "Trigger a reconciliation job",
            description = "Compares internal ledger records against the provider's settlement file for the given date. " +
                          "If no date is provided, defaults to today. Returns the job summary — check the entries endpoint for line-level detail."
    )
    public ResponseEntity<ReconciliationJob> runReconciliation(
            @RequestParam(required = false) String date) {
        ReconciliationJob job = reconciliationService.runReconciliation(date);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping("/jobs")
    @Operation(summary = "List recent reconciliation jobs",
               description = "Returns the 10 most recent jobs, newest first.")
    public ResponseEntity<List<ReconciliationJob>> getRecentJobs() {
        return ResponseEntity.ok(reconciliationService.getRecentJobs());
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get a reconciliation job by ID")
    public ResponseEntity<ReconciliationJob> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(reconciliationService.getJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/entries")
    @Operation(summary = "Get all entries for a reconciliation job",
               description = "Returns line-level results — each entry shows MATCHED, MISMATCH, or UNMATCHED with a discrepancy note.")
    public ResponseEntity<List<ReconciliationEntry>> getEntries(@PathVariable UUID jobId) {
        return ResponseEntity.ok(reconciliationService.getEntries(jobId));
    }
}
