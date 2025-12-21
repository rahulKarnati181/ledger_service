package com.wallet.ledger_service.Domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    public enum moneyDirection {
        CREDIT, DEBIT;
    }
    public enum  moneyType{
        PAYMENT,PAYOUT, REFUND,ADJUSTED;
    }

    @Id
    private String id;
    private String accountId;
    private String counterpartyAccountId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private moneyDirection direction;
    @Enumerated(EnumType.STRING)
    private moneyType type;
    private String referenceType;
    private String referenceId;
    private Instant createdAt;
}
