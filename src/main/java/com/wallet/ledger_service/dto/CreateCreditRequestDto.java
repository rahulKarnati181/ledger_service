package com.wallet.ledger_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCreditRequestDto {
    private BigDecimal amount;
    private String referenceType;
    private String referenceId;
}
