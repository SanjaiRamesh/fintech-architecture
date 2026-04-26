package com.payment.fx.service;

import com.payment.fx.entity.FxRate;
import com.payment.fx.repository.FxRateRepository;
import com.payment.shared.dto.response.FxConversionResult;
import com.payment.shared.dto.response.FxRateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FxService {

    private final FxRateRepository fxRateRepository;

    @Value("${fx.spread:0.005}")
    private BigDecimal spread;

    public FxConversionResult convert(String paymentId, BigDecimal amount,
                                       String fromCurrency, String toCurrency) {
        log.debug("Converting {} {} → {} for paymentId={}", amount, fromCurrency, toCurrency, paymentId);

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return FxConversionResult.builder()
                    .paymentId(paymentId)
                    .originalAmount(amount)
                    .originalCurrency(fromCurrency)
                    .convertedAmount(amount)
                    .convertedCurrency(toCurrency)
                    .rate(BigDecimal.ONE)
                    .convertedAt(Instant.now())
                    .build();
        }

        FxRate fxRate = fxRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency.toUpperCase(), toCurrency.toUpperCase())
                .orElseThrow(() -> new FxRateNotFoundException(fromCurrency, toCurrency));

        // Apply spread: effective rate = mid-market rate * (1 - spread)
        BigDecimal effectiveRate = fxRate.getRate()
                .multiply(BigDecimal.ONE.subtract(spread))
                .setScale(8, RoundingMode.HALF_UP);

        BigDecimal convertedAmount = amount.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);

        log.info("FX conversion paymentId={} {} {} → {} {} at rate {}",
                paymentId, amount, fromCurrency, convertedAmount, toCurrency, effectiveRate);

        return FxConversionResult.builder()
                .paymentId(paymentId)
                .originalAmount(amount)
                .originalCurrency(fromCurrency)
                .convertedAmount(convertedAmount)
                .convertedCurrency(toCurrency)
                .rate(effectiveRate)
                .convertedAt(Instant.now())
                .build();
    }

    public List<FxRateResult> getAllRates() {
        return fxRateRepository.findAll().stream()
                .map(r -> FxRateResult.builder()
                        .from(r.getFromCurrency())
                        .to(r.getToCurrency())
                        .rate(r.getRate())
                        .validUntil(r.getUpdatedAt().plusSeconds(3600))
                        .source("MID_MARKET")
                        .build())
                .toList();
    }

    public static class FxRateNotFoundException extends RuntimeException {
        public FxRateNotFoundException(String from, String to) {
            super("No FX rate found for " + from + " → " + to);
        }
    }
}
