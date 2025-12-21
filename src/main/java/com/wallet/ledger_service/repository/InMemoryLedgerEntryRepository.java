package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.LedgerEntry;
import org.springframework.stereotype.Repository;

import java.util.*;



public class InMemoryLedgerEntryRepository implements LedgerEntryRepository {
    private final Map<String, List<LedgerEntry>> accounts = new HashMap<>();

    @Override
    public void saveAll(List<LedgerEntry> entries) {
        for(int i=0;i<entries.size();i++) {
            LedgerEntry entry = entries.get(i);
            String accountId =entry.getAccountId();
            if(accounts.containsKey(accountId)) {
                accounts.get(accountId).add(entry);
            }else{
                List<LedgerEntry> newEntries = new ArrayList<>();
                newEntries.add(entry);
                accounts.put(accountId, newEntries);
            }
        }
    }

    @Override
    public List<LedgerEntry> findByAccountId(String accountId,int page,int size){
        List<LedgerEntry> entries = accounts.get(accountId);
        if(entries == null) {
            return Collections.emptyList();
        }
        int start =page*size;
        int end= Math.min(start+size,entries.size());
        if(start >= entries.size()) {
            return Collections.emptyList();
        }
        List<LedgerEntry> results = new ArrayList<>();
        for(int i=start;i<end;i++) {
            LedgerEntry entry = entries.get(i);
            results.add(entry);
        }

        return results;
    }



}
