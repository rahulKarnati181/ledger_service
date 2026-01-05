package com.wallet.ledger_service.dto;

import com.wallet.ledger_service.Domain.PaymentIntent;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutRequestDto {
    String accountId;
    BigDecimal amount;
    String currency;
    PaymentIntent.Provider provider;
}
