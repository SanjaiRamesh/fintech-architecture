package com.payment.reconciliation.service;

import com.payment.reconciliation.entity.ReconciliationEntry;
import com.payment.reconciliation.entity.ReconciliationJob;
import com.payment.reconciliation.repository.ReconciliationEntryRepository;
import com.payment.reconciliation.repository.ReconciliationJobRepository;
import com.payment.shared.enums.ReconciliationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationJobRepository jobRepository;
    private final ReconciliationEntryRepository entryRepository;

    @Transactional
    public ReconciliationJob runReconciliation(String reconciliationDate) {
        String date = reconciliationDate != null ? reconciliationDate : LocalDate.now().toString();
        log.info("Starting reconciliation for date={}", date);

        ReconciliationJob job = ReconciliationJob.builder()
                .triggeredAt(Instant.now())
                .reconciliationDate(date)
                .overallStatus(ReconciliationStatus.MATCHED)
                .build();
        job = jobRepository.save(job);

        // Simulate reconciliation: generate representative entries
        List<ReconciliationEntry> entries = generateSimulatedEntries(job.getId());
        entryRepository.saveAll(entries);

        long matched = entries.stream().filter(e -> e.getStatus() == ReconciliationStatus.MATCHED).count();
        long mismatched = entries.stream().filter(e -> e.getStatus() == ReconciliationStatus.MISMATCH).count();
        long unmatched = entries.stream().filter(e -> e.getStatus() == ReconciliationStatus.UNMATCHED).count();

        ReconciliationStatus overall = unmatched > 0 || mismatched > 0
                ? ReconciliationStatus.MISMATCH
                : ReconciliationStatus.MATCHED;

        job.setTotalRecords(entries.size());
        job.setMatchedCount((int) matched);
        job.setMismatchCount((int) mismatched);
        job.setUnmatchedCount((int) unmatched);
        job.setOverallStatus(overall);
        job.setCompletedAt(Instant.now());
        job = jobRepository.save(job);

        log.info("Reconciliation completed: jobId={} matched={} mismatch={} unmatched={}",
                job.getId(), matched, mismatched, unmatched);
        return job;
    }

    public ReconciliationJob getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public List<ReconciliationJob> getRecentJobs() {
        return jobRepository.findTop10ByOrderByTriggeredAtDesc();
    }

    public List<ReconciliationEntry> getEntries(UUID jobId) {
        return entryRepository.findByJobId(jobId);
    }

    // Simulates 10 records: 8 matched, 1 amount mismatch, 1 unmatched
    private List<ReconciliationEntry> generateSimulatedEntries(UUID jobId) {
        List<ReconciliationEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            BigDecimal amount = BigDecimal.valueOf(100L * i);
            entries.add(entry(jobId, "PMT-" + i, "PROV-REF-" + i,
                    amount, amount, "EUR", ReconciliationStatus.MATCHED, null));
        }
        // Amount mismatch: internal says 900, provider says 890
        entries.add(entry(jobId, "PMT-9", "PROV-REF-9",
                new BigDecimal("900.00"), new BigDecimal("890.00"), "EUR",
                ReconciliationStatus.MISMATCH,
                "Provider amount £890.00 does not match internal record £900.00"));
        // Unmatched: no provider record found
        entries.add(entry(jobId, "PMT-10", null,
                new BigDecimal("1500.00"), null, "USD",
                ReconciliationStatus.UNMATCHED,
                "No matching record found in provider settlement file"));
        return entries;
    }

    private ReconciliationEntry entry(UUID jobId, String paymentId, String providerRef,
                                       BigDecimal internal, BigDecimal provider, String currency,
                                       ReconciliationStatus status, String note) {
        return ReconciliationEntry.builder()
                .jobId(jobId)
                .paymentId(paymentId)
                .providerReference(providerRef)
                .internalAmount(internal)
                .providerAmount(provider)
                .currency(currency)
                .status(status)
                .discrepancyNote(note)
                .reconciledAt(Instant.now())
                .build();
    }

    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(UUID jobId) {
            super("Reconciliation job not found: " + jobId);
        }
    }
}
