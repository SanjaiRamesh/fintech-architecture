package com.payment.fx.init;

import com.payment.fx.entity.FxRate;
import com.payment.fx.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateInitializer implements CommandLineRunner {

    private final FxRateRepository fxRateRepository;

    @Override
    public void run(String... args) {
        if (fxRateRepository.count() > 0) {
            log.info("FX rates already seeded, skipping");
            return;
        }
        log.info("Seeding FX rates...");
        fxRateRepository.saveAll(rates());
        log.info("Seeded {} FX rates", fxRateRepository.count());
    }

    private List<FxRate> rates() {
        Instant now = Instant.now();
        return List.of(
                rate("USD", "EUR", "0.92000000", now),
                rate("USD", "GBP", "0.79000000", now),
                rate("USD", "JPY", "149.80000000", now),
                rate("USD", "CHF", "0.89000000", now),
                rate("USD", "CAD", "1.36000000", now),
                rate("USD", "AUD", "1.53000000", now),
                rate("EUR", "USD", "1.08700000", now),
                rate("EUR", "GBP", "0.85800000", now),
                rate("EUR", "JPY", "162.80000000", now),
                rate("EUR", "CHF", "0.96700000", now),
                rate("GBP", "USD", "1.26500000", now),
                rate("GBP", "EUR", "1.16500000", now),
                rate("GBP", "JPY", "189.70000000", now),
                rate("JPY", "USD", "0.00668000", now),
                rate("JPY", "EUR", "0.00614000", now),
                rate("CHF", "USD", "1.12400000", now),
                rate("CHF", "EUR", "1.03400000", now)
        );
    }

    private FxRate rate(String from, String to, String rateStr, Instant now) {
        return FxRate.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .rate(new BigDecimal(rateStr))
                .updatedAt(now)
                .build();
    }
}
