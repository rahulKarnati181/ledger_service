package com.wallet.ledger_service.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider{

    private final Map<String ,ProviderPaymentCreationResult> idempotencyMap=new ConcurrentHashMap<>();

    @Override
    public ProviderPaymentCreationResult createPayment(String idempotencyKey,
                                                       BigDecimal amount, String currency) {
        return(idempotencyMap.computeIfAbsent(
                idempotencyKey,k -> new ProviderPaymentCreationResult
                        ("mock_Id" + UUID.randomUUID()))
        );

    }

    @Override
    public ProviderWebhookResult interpretWebhook(String payload) {
        String providerEventId = extract(payload, "providerEventId");
        String providerPaymentId = extract(payload, "providerPaymentId");
        String status = extract(payload, "status");

        ProviderWebhookResult.ResultStatus resultStatus =
                ProviderWebhookResult.ResultStatus.valueOf(status);

        return new ProviderWebhookResult(
                providerEventId,
                providerPaymentId,
                resultStatus
        );
    }
    private String extract(String payload, String key) {
        // VERY naive parser for MOCK only
        String token = "\"" + key + "\":";
        int start = payload.indexOf(token) + token.length();
        int firstQuote = payload.indexOf("\"", start) + 1;
        int secondQuote = payload.indexOf("\"", firstQuote);
        return payload.substring(firstQuote, secondQuote);
    }
}
