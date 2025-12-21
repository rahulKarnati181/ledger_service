package com.wallet.ledger_service.dto;

import lombok.Data;

@Data
public class CreateAccountRequestDto {
    private String merchantId;
    private String currency;
}
