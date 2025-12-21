package com.wallet.ledger_service.dto;

import com.wallet.ledger_service.Domain.Account;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalanceDto {

    public enum AccountStatus {
        ACTIVE,INACTIVE, DELETED;
    }
    public String accountID;
    public BigDecimal currentBalance;
    public String currency;
    private Account.AccountStatus status;
}
