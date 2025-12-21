package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.Account;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new HashMap<>();

    @Override
    public Account save(Account account) {
        accounts.put(account.getAccountId(), account);
        return account;
    }
    @Override
    public Optional<Account> findById(String accountId){
        Account account = accounts.get(accountId);
        return Optional.ofNullable(account);
    }
    @Override
    public Optional<Account> findActiveByMerchantIdAndCurrency(String merchantId, String currency) {
        for (Map.Entry<String, Account> entry : accounts.entrySet()) {
            Account account = entry.getValue();
            if(account.getMerchantId().equals(merchantId) && account.getCurrency().equals(currency)
                    && account.getStatus() == Account.AccountStatus.ACTIVE ) {
                return Optional.of(account);
            }
        }
        return Optional.empty();
    }



}
