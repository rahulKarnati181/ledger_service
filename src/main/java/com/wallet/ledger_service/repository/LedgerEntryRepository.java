package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.LedgerEntry;
import org.springframework.stereotype.Repository;

import java.util.List;



public interface LedgerEntryRepository {
    void saveAll(List<LedgerEntry> entries);
    List<LedgerEntry> findByAccountId(String accountId,int page,int size);
}
