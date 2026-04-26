package com.payment.validation.validator;

import com.payment.shared.dto.response.ValidationError;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;

@Component
public class AmountValidator {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");

    // Currencies that don't use decimal fractions (e.g. JPY, KRW)
    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of("JPY", "KRW", "VND", "IDR");

    public List<ValidationError> validate(BigDecimal amount, String currency) {
        List<ValidationError> errors = new ArrayList<>();

        if (amount == null) {
            errors.add(new ValidationError("amount", "AMOUNT_REQUIRED", "Payment amount is required"));
            return errors;
        }
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            errors.add(new ValidationError("amount", "AMOUNT_TOO_SMALL",
                    "Amount must be at least " + MIN_AMOUNT));
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            errors.add(new ValidationError("amount", "AMOUNT_EXCEEDS_LIMIT",
                    "Amount exceeds single-transaction limit of " + MAX_AMOUNT));
        }
        if (currency != null && !ZERO_DECIMAL_CURRENCIES.contains(currency)) {
            if (amount.scale() > 2) {
                errors.add(new ValidationError("amount", "INVALID_DECIMAL_PLACES",
                        "Amount for " + currency + " must have at most 2 decimal places"));
            }
        }
        return errors;
    }

    public List<ValidationError> validateCurrency(String currency) {
        List<ValidationError> errors = new ArrayList<>();
        if (currency == null || currency.isBlank()) {
            errors.add(new ValidationError("currency", "CURRENCY_REQUIRED", "Currency code is required"));
            return errors;
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            errors.add(new ValidationError("currency", "INVALID_CURRENCY_CODE",
                    "'" + currency + "' is not a valid ISO 4217 currency code"));
        }
        return errors;
    }
}
