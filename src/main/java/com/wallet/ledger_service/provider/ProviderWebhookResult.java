package com.wallet.ledger_service.provider;

public record ProviderWebhookResult( String providerEventId,
                                     String providerPaymentId,
                                     ResultStatus status) {
    public enum ResultStatus {
        SUCCEEDED,
        FAILED,
        CANCELLED
    }
}
