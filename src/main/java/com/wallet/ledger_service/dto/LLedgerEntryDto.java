package com.wallet.ledger_service.dto;

import com.wallet.ledger_service.Domain.LedgerEntry;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class LLedgerEntryDto {
    public enum moneyDirection {
        CREDIT, DEBIT;
    }
    public enum  moneyType{
        PAYMENT,PAYOUT, REFUND,ADJUSTED;
    }

    private String id;
    private String accountId;
    private String counterpartyAccountId;
    private BigDecimal amount;
    private LedgerEntry.moneyDirection direction;
    private LedgerEntry.moneyType type;
    private String referenceType;
    private String referenceId;
    private Instant createdAt;
    private int page;
    private int size;
    private boolean hasNext;
}
