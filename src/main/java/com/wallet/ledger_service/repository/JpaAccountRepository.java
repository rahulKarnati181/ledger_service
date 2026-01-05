package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.Account;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaAccountRepository implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    JpaAccountRepository(AccountJpaRepository accountJpaRepository){
        this.accountJpaRepository=accountJpaRepository;
    }

    @Override
    public Account save(Account account){
        return(accountJpaRepository.save(account));
    }

    @Override
    public Optional<Account> findById(String accountId){
        return(accountJpaRepository.findById(accountId));
    }

    @Override
    public Optional<Account> findActiveByMerchantIdAndCurrency(String merchantId, String currency){
        return(accountJpaRepository.findByMerchantIdAndCurrencyAndStatus
                (merchantId,currency, Account.AccountStatus.ACTIVE));
    }

}
