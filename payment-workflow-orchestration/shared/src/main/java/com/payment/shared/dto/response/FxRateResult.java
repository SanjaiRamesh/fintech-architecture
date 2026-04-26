package com.payment.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRateResult {

    private String from;
    private String to;
    private BigDecimal rate;
    private Instant validUntil;
    private String source;
}
