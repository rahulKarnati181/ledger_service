package com.wallet.ledger_service.Domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name= "accounts")
public class Account {
    public enum AccountStatus {
        ACTIVE,INACTIVE, DELETED;
    }
    @Id
    private String accountId;
    private String merchantId;
    private String currency;
    private BigDecimal currentBalance;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    private Instant createdAt;


}
