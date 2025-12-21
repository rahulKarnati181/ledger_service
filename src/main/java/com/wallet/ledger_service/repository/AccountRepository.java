package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.Account;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(String accountId);

    Optional<Account> findActiveByMerchantIdAndCurrency(String merchantId, String currency);


}
