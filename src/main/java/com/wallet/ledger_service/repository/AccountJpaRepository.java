package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<Account,String> {
    Optional<Account> findByMerchantIdAndCurrencyAndStatus(String merchantId, String currency, Account.AccountStatus status);

}
