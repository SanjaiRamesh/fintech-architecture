package com.payment.shared.exception;

import com.payment.shared.enums.Provider;

public class ProviderException extends PaymentException {

    private final Provider provider;

    public ProviderException(String errorCode, Provider provider, String message) {
        super(errorCode, message);
        this.provider = provider;
    }

    public ProviderException(String errorCode, Provider provider, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.provider = provider;
    }

    public Provider getProvider() {
        return provider;
    }

    public static ProviderException unavailable(Provider provider) {
        return new ProviderException("PROVIDER_UNAVAILABLE", provider,
                "Provider unreachable: " + provider);
    }

    public static ProviderException rejected(Provider provider, String reason) {
        return new ProviderException("PROVIDER_REJECTED", provider,
                "Provider rejected payment: " + provider + " — " + reason);
    }

    public static ProviderException timeout(Provider provider) {
        return new ProviderException("PROVIDER_TIMEOUT", provider,
                "Provider timed out: " + provider);
    }
}
