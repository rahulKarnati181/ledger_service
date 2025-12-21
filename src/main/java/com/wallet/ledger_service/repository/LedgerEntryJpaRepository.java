package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntry,String> {
    Page<LedgerEntry> findByAccountId(String accountId, Pageable pageable);


}
