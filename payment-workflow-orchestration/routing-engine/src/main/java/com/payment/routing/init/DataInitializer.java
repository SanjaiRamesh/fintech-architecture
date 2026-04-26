package com.payment.routing.init;

import com.payment.routing.entity.ProviderConfig;
import com.payment.routing.repository.ProviderConfigRepository;
import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.enums.PaymentRail;
import com.payment.shared.enums.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProviderConfigRepository providerConfigRepository;

    @Override
    public void run(String... args) {
        if (providerConfigRepository.count() > 0) {
            log.info("Provider configs already seeded, skipping");
            return;
        }
        log.info("Seeding provider configurations...");
        providerConfigRepository.saveAll(providerConfigs());
        log.info("Seeded {} provider configurations", providerConfigRepository.count());
    }

    private List<ProviderConfig> providerConfigs() {
        return List.of(
                // Deutsche Bank — SEPA standard (T+1)
                ProviderConfig.builder()
                        .provider(Provider.DEUTSCHE_BANK)
                        .rail(PaymentRail.SEPA)
                        .supportedCurrencies(List.of("EUR"))
                        .supportedCountries(List.of("DE", "FR", "NL", "BE", "AT", "ES", "IT", "PT"))
                        .supportedMethods(List.of(PaymentMethod.BANK_TRANSFER))
                        .priority(1)
                        .active(true)
                        .minAmount(new BigDecimal("0.01"))
                        .maxAmount(new BigDecimal("1000000.00"))
                        .settlementHours(24)
                        .build(),

                // Deutsche Bank — SEPA Instant (< 10 seconds)
                ProviderConfig.builder()
                        .provider(Provider.DEUTSCHE_BANK)
                        .rail(PaymentRail.SEPA_INSTANT)
                        .supportedCurrencies(List.of("EUR"))
                        .supportedCountries(List.of("DE", "FR", "NL", "BE", "AT", "ES", "IT", "PT"))
                        .supportedMethods(List.of(PaymentMethod.BANK_TRANSFER))
                        .priority(1)
                        .active(true)
                        .minAmount(new BigDecimal("0.01"))
                        .maxAmount(new BigDecimal("100000.00"))
                        .settlementHours(0)
                        .build(),

                // Barclays — SEPA (fallback for EUR, covers GBP corridor too)
                ProviderConfig.builder()
                        .provider(Provider.BARCLAYS)
                        .rail(PaymentRail.SEPA)
                        .supportedCurrencies(List.of("EUR", "GBP"))
                        .supportedCountries(List.of("GB", "DE", "FR", "NL", "BE", "AT", "ES", "IT"))
                        .supportedMethods(List.of(PaymentMethod.BANK_TRANSFER))
                        .priority(2)
                        .active(true)
                        .minAmount(new BigDecimal("0.01"))
                        .maxAmount(new BigDecimal("1000000.00"))
                        .settlementHours(24)
                        .build(),

                // JPMorgan — SWIFT for international wire transfers (T+3)
                ProviderConfig.builder()
                        .provider(Provider.JPMORGAN)
                        .rail(PaymentRail.SWIFT)
                        .supportedCurrencies(List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"))
                        .supportedCountries(null) // global
                        .supportedMethods(List.of(PaymentMethod.BANK_TRANSFER))
                        .priority(1)
                        .active(true)
                        .minAmount(new BigDecimal("100.00"))
                        .maxAmount(new BigDecimal("10000000.00"))
                        .settlementHours(72)
                        .build(),

                // Visa Direct — instant card push payments
                ProviderConfig.builder()
                        .provider(Provider.VISA)
                        .rail(PaymentRail.VISA_DIRECT)
                        .supportedCurrencies(List.of("USD", "EUR", "GBP"))
                        .supportedCountries(null) // global
                        .supportedMethods(List.of(PaymentMethod.CARD))
                        .priority(1)
                        .active(true)
                        .minAmount(new BigDecimal("0.01"))
                        .maxAmount(new BigDecimal("50000.00"))
                        .settlementHours(0)
                        .build(),

                // Mastercard Send — instant card push payments
                ProviderConfig.builder()
                        .provider(Provider.MASTERCARD)
                        .rail(PaymentRail.MASTERCARD_SEND)
                        .supportedCurrencies(List.of("USD", "EUR", "GBP"))
                        .supportedCountries(null) // global
                        .supportedMethods(List.of(PaymentMethod.CARD))
                        .priority(2)
                        .active(true)
                        .minAmount(new BigDecimal("0.01"))
                        .maxAmount(new BigDecimal("50000.00"))
                        .settlementHours(0)
                        .build(),

                // PayPal — digital wallet
                ProviderConfig.builder()
                        .provider(Provider.PAYPAL)
                        .rail(PaymentRail.PAYPAL)
                        .supportedCurrencies(List.of("USD", "EUR", "GBP"))
                        .supportedCountries(null) // global
                        .supportedMethods(List.of(PaymentMethod.WALLET))
                        .priority(1)
                        .active(true)
                        .minAmount(new BigDecimal("0.01"))
                        .maxAmount(new BigDecimal("10000.00"))
                        .settlementHours(0)
                        .build()
        );
    }
}
