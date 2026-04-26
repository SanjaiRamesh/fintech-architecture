package com.payment.routing.service;

import com.payment.routing.entity.ProviderConfig;
import com.payment.routing.entity.RoutingLog;
import com.payment.routing.repository.ProviderConfigRepository;
import com.payment.routing.repository.RoutingLogRepository;
import com.payment.shared.dto.response.RoutingDecision;
import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.exception.RoutingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final ProviderConfigRepository providerConfigRepository;
    private final RoutingLogRepository routingLogRepository;

    @Transactional
    public RoutingDecision route(RoutingRequest request) {
        log.debug("Routing payment paymentId={} currency={} method={}",
                request.paymentId(), request.currency(), request.paymentMethod());

        // Use source currency for routing if provided (we route based on what provider settles in)
        String routingCurrency = request.sourceCurrency() != null
                ? request.sourceCurrency()
                : request.currency();

        String currencyJson = "[\"" + routingCurrency + "\"]";
        String methodJson = "[\"" + request.paymentMethod().name() + "\"]";

        List<ProviderConfig> candidates = providerConfigRepository
                .findEligible(currencyJson, methodJson);

        // Filter by country if the provider has country restrictions
        if (request.beneficiaryCountry() != null && !request.beneficiaryCountry().isBlank()) {
            candidates = candidates.stream()
                    .filter(c -> c.getSupportedCountries() == null
                            || c.getSupportedCountries().contains(request.beneficiaryCountry()))
                    .collect(Collectors.toList());
        }

        // Filter by amount range
        if (request.amount() != null) {
            candidates = candidates.stream()
                    .filter(c -> (c.getMinAmount() == null || request.amount().compareTo(c.getMinAmount()) >= 0)
                            && (c.getMaxAmount() == null || request.amount().compareTo(c.getMaxAmount()) <= 0))
                    .collect(Collectors.toList());
        }

        if (candidates.isEmpty()) {
            log.warn("No routing candidates for paymentId={} currency={} method={}",
                    request.paymentId(), routingCurrency, request.paymentMethod());
            throw new RoutingException(request.paymentId(), "No active provider supports this payment");
        }

        ProviderConfig primary = candidates.get(0);
        // Fallback: first candidate from a different provider
        ProviderConfig fallback = candidates.stream()
                .filter(c -> c.getProvider() != primary.getProvider())
                .findFirst()
                .orElse(null);

        Instant estimatedSettlement = primary.getSettlementHours() == 0
                ? Instant.now().plusSeconds(30)
                : Instant.now().plus(primary.getSettlementHours(), ChronoUnit.HOURS);

        String criteria = "currency=" + routingCurrency
                + " method=" + request.paymentMethod()
                + " country=" + request.beneficiaryCountry()
                + " amount=" + request.amount();

        RoutingLog log_ = RoutingLog.builder()
                .paymentId(request.paymentId())
                .selectedProvider(primary.getProvider())
                .selectedRail(primary.getRail())
                .fallbackProvider(fallback != null ? fallback.getProvider() : null)
                .routedAt(Instant.now())
                .routingCriteria(criteria)
                .build();
        routingLogRepository.save(log_);

        log.info("Routed paymentId={} to provider={} rail={}",
                request.paymentId(), primary.getProvider(), primary.getRail());

        return RoutingDecision.builder()
                .paymentId(request.paymentId())
                .provider(primary.getProvider())
                .rail(primary.getRail())
                .priority(primary.getPriority())
                .fallbackProvider(fallback != null ? fallback.getProvider() : null)
                .estimatedSettlementTime(estimatedSettlement)
                .routedAt(Instant.now())
                .build();
    }

    public record RoutingRequest(
            String paymentId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            String sourceCurrency,
            String beneficiaryCountry
    ) {}
}
