package com.payment.routing.repository;

import com.payment.routing.entity.ProviderConfig;
import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, UUID> {

    List<ProviderConfig> findByActiveOrderByPriorityAsc(boolean active);

    boolean existsByProvider(Provider provider);

    // Finds active configs that support the given currency (JSONB contains check)
    @Query(value = """
            SELECT * FROM provider_configs
            WHERE active = true
              AND supported_currencies @> :currency::jsonb
              AND supported_methods @> :method::jsonb
            ORDER BY priority ASC
            """, nativeQuery = true)
    List<ProviderConfig> findEligible(@Param("currency") String currency,
                                      @Param("method") String method);
}
