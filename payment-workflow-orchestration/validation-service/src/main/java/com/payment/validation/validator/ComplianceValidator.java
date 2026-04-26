package com.payment.validation.validator;

import com.payment.shared.dto.response.ValidationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ComplianceValidator {

    // OFAC / EU sanctions — currencies not accepted on this platform
    private static final Set<String> SANCTIONED_CURRENCIES = Set.of("IRR", "KPW", "SYP");

    // High-risk country codes (ISO 3166-1 alpha-2) — require additional review flags
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of("IR", "KP", "SY", "CU", "SD");

    public List<ValidationError> validateCurrencyCompliance(String currency) {
        List<ValidationError> errors = new ArrayList<>();
        if (currency != null && SANCTIONED_CURRENCIES.contains(currency.toUpperCase())) {
            errors.add(new ValidationError("currency", "SANCTIONED_CURRENCY",
                    "Transactions in " + currency + " are not permitted under current compliance rules"));
        }
        return errors;
    }

    public List<ValidationError> validateCountryCompliance(String countryCode) {
        List<ValidationError> errors = new ArrayList<>();
        if (countryCode == null || countryCode.isBlank()) {
            return errors;
        }
        String normalized = countryCode.toUpperCase();
        if (HIGH_RISK_COUNTRIES.contains(normalized)) {
            errors.add(new ValidationError("beneficiary.country", "HIGH_RISK_COUNTRY",
                    "Payments to country " + normalized + " are blocked under OFAC/EU sanctions"));
        }
        return errors;
    }

    public List<ValidationError> validateDescription(String description) {
        List<ValidationError> errors = new ArrayList<>();
        if (description != null && description.length() > 140) {
            errors.add(new ValidationError("description", "DESCRIPTION_TOO_LONG",
                    "Payment description must not exceed 140 characters"));
        }
        return errors;
    }
}
