package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaLedgerEntryRepository implements LedgerEntryRepository {
    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;

    JpaLedgerEntryRepository(LedgerEntryJpaRepository ledgerEntryJpaRepository){
        this.ledgerEntryJpaRepository=ledgerEntryJpaRepository;
    }

    @Override
    public void saveAll(List<LedgerEntry> entries){
        ledgerEntryJpaRepository.saveAll(entries);
    }

    @Override
    public List<LedgerEntry> findByAccountId(String accountId,int page,int size){
        Pageable pageable= PageRequest.of(page,size);
        Page<LedgerEntry> resultPage=ledgerEntryJpaRepository.findByAccountId(accountId,pageable);
        return resultPage.getContent();
    }

}
