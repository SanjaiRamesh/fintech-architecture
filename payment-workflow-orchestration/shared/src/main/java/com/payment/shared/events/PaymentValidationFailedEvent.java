package com.payment.shared.events;

import com.payment.shared.dto.response.ValidationError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentValidationFailedEvent extends BaseEvent {

    private String paymentId;
    private List<ValidationError> errors;

    public static PaymentValidationFailedEvent create(String paymentId, List<ValidationError> errors) {
        return PaymentValidationFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentValidationFailed")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .errors(errors)
                .build();
    }
}
