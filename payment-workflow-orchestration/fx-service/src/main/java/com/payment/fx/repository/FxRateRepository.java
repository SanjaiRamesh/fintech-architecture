package com.payment.fx.repository;

import com.payment.fx.entity.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FxRateRepository extends JpaRepository<FxRate, UUID> {

    Optional<FxRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
