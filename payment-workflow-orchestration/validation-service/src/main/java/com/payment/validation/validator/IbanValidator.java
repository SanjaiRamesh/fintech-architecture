package com.payment.validation.validator;

import com.payment.shared.dto.response.ValidationError;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class IbanValidator {

    private static final Pattern IBAN_PATTERN = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{1,30}");
    private static final Pattern BIC_PATTERN = Pattern.compile("[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?");

    public List<ValidationError> validateIban(String iban) {
        List<ValidationError> errors = new ArrayList<>();
        if (iban == null || iban.isBlank()) {
            return errors; // IBAN is optional for non-bank-transfer payments
        }
        String normalized = iban.replaceAll("\\s", "").toUpperCase();
        if (!IBAN_PATTERN.matcher(normalized).matches()) {
            errors.add(new ValidationError("beneficiary.iban", "INVALID_IBAN_FORMAT",
                    "IBAN does not match expected format (e.g. DE89370400440532013000)"));
            return errors;
        }
        if (!passesmod97(normalized)) {
            errors.add(new ValidationError("beneficiary.iban", "IBAN_CHECKSUM_FAILED",
                    "IBAN checksum (MOD-97) validation failed"));
        }
        return errors;
    }

    public List<ValidationError> validateBic(String bic) {
        List<ValidationError> errors = new ArrayList<>();
        if (bic == null || bic.isBlank()) {
            return errors;
        }
        String normalized = bic.replaceAll("\\s", "").toUpperCase();
        if (!BIC_PATTERN.matcher(normalized).matches()) {
            errors.add(new ValidationError("beneficiary.bic", "INVALID_BIC_FORMAT",
                    "BIC must be 8 or 11 characters (e.g. DEUTDEDB or DEUTDEDBFRA)"));
        }
        return errors;
    }

    // MOD-97 algorithm: move first 4 chars to end, replace letters with digits, compute mod 97
    private boolean passesmod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(c - 'A' + 10);
            } else {
                numeric.append(c);
            }
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
