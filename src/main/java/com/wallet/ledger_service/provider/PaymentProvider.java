package com.wallet.ledger_service.provider;

import java.math.BigDecimal;

public interface PaymentProvider {
    ProviderPaymentCreationResult createPayment(
            String paymentIntentId,
            BigDecimal amount,
            String currency
    );

    ProviderWebhookResult interpretWebhook(String payload);
}
