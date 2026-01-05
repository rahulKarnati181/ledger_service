package com.wallet.ledger_service.dto;

import com.wallet.ledger_service.Domain.PaymentIntent;
import lombok.Data;

@Data
public class CheckoutResponseDto {
    String PaymentIntentId;
    PaymentIntent.PaymentStatus status;
    String providerPaymentId;
    boolean idempotencyHit;
}
