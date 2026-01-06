package com.wallet.ledger_service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private final Map<String, ProviderPaymentCreationResult> idempotencyMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public MockPaymentProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderPaymentCreationResult createPayment(String idempotencyKey,
                                                       BigDecimal amount,
                                                       String currency) {
        return idempotencyMap.computeIfAbsent(
                idempotencyKey,
                k -> new ProviderPaymentCreationResult("pi_" + UUID.randomUUID())
        );
    }

    @Override
    public ProviderWebhookResult interpretWebhook(String payload) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload", e);
        }

        String eventId = requireText(root, "id");
        String type = requireText(root, "type");
        String providerPaymentId = requireText(root.at("/data/object"), "id");

        if (!eventId.startsWith("evt_")) {
            throw new IllegalArgumentException("Invalid event id: " + eventId);
        }
        if (!providerPaymentId.startsWith("pi_")) {
            throw new IllegalArgumentException("Invalid payment intent id: " + providerPaymentId);
        }

        ProviderWebhookResult.ResultStatus status = mapStripeTypeToStatus(type);

        return new ProviderWebhookResult(
                eventId,
                providerPaymentId,
                status
        );
    }

    private ProviderWebhookResult.ResultStatus mapStripeTypeToStatus(String type) {
        return switch (type) {
            case "payment_intent.succeeded" -> ProviderWebhookResult.ResultStatus.SUCCEEDED;
            case "payment_intent.payment_failed" -> ProviderWebhookResult.ResultStatus.FAILED;
            case "payment_intent.canceled" -> ProviderWebhookResult.ResultStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unhandled event type: " + type);
        };
    }

    private static String requireText(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("Missing node for field: " + field);
        }
        JsonNode val = node.get(field);
        if (val == null || val.isNull() || val.asText().isBlank()) {
            throw new IllegalArgumentException("Missing/blank field: " + field);
        }
        return val.asText();
    }
}
