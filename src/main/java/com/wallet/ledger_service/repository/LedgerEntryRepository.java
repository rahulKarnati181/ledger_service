package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.LedgerEntry;

import java.util.List;
import java.util.Optional;


public interface LedgerEntryRepository {
    void saveAll(List<LedgerEntry> entries);

    Optional<LedgerEntry> findByAccountIdAndReferenceTypeAndReferenceIdAndDirection(
            String accountId,
            String referenceType,
            String referenceId,
            LedgerEntry.moneyDirection direction
    );

    List<LedgerEntry> findByAccountId(String accountId,int page,int size);
}
