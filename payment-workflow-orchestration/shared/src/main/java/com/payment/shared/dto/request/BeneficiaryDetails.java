package com.payment.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryDetails {

    private String accountId;

    @NotBlank
    private String accountName;

    private String iban;
    private String bankCode;
    private String country;
}
