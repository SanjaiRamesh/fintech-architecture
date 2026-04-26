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
public class AccountDetails {

    @NotBlank
    private String accountId;

    private String accountName;
    private String bankCode;
    private String iban;
}
