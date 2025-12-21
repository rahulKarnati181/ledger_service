package com.wallet.ledger_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreditDto {


    private String accountId;
    private BigDecimal newBalance;
    private BigDecimal amount;
    private String referenceType;
    private String referenceId;
    private String operationId;
    private Instant timestamp;

}
