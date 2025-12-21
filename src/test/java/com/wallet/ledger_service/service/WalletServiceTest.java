package com.wallet.ledger_service.service;

import com.wallet.ledger_service.Domain.Account;
import com.wallet.ledger_service.Domain.LedgerEntry;
import com.wallet.ledger_service.dto.AccountBalanceDto;
import com.wallet.ledger_service.dto.CreditDto;
import com.wallet.ledger_service.exception.ConflictException;
import com.wallet.ledger_service.repository.AccountRepository;
import com.wallet.ledger_service.repository.InMemoryAccountRepository;
import com.wallet.ledger_service.repository.InMemoryLedgerEntryRepository;
import com.wallet.ledger_service.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WalletServiceTest {
    private  AccountRepository accountRepository;
    private  LedgerEntryRepository ledgerEntryRepository;
    private  WalletService walletService;

    @BeforeEach
    void setUp(){
        accountRepository=new InMemoryAccountRepository();
        ledgerEntryRepository=new InMemoryLedgerEntryRepository();
        walletService=new WalletService(accountRepository,
                ledgerEntryRepository);
    }

    @Test
    void checkCreditAndDebitBalanceThenLedgerEntry(){

        Account account= walletService.createAccount("M1","INR");
        String accountId= account.getAccountId();

        CreditDto creditDto=walletService.credit(accountId, BigDecimal.valueOf(1250),
                "PAYMENT","PO_1001");
        CreditDto debitDto=walletService.debit(accountId, BigDecimal.valueOf(800),
                "PAYOUT","PO_2001");
        AccountBalanceDto accountBalanceDto=walletService.getBalance(accountId);
        BigDecimal balance=accountBalanceDto.getCurrentBalance();
        assertEquals(0, BigDecimal.valueOf(450).compareTo(balance));

        List<LedgerEntry> entries=ledgerEntryRepository.findByAccountId(
                accountId, 0, 10);
        assertEquals(2, entries.size());
        List<LedgerEntry> entriesPlatform=ledgerEntryRepository.findByAccountId(
                "PLATFORM", 0, 10);
        assertEquals(2,entriesPlatform.size());
        LedgerEntry creditEntry=null;
        LedgerEntry debitEntry=null;
        for(LedgerEntry entry : entries){
            if(entry.getDirection()== LedgerEntry.moneyDirection.CREDIT){
                creditEntry=entry;
            }
            if(entry.getDirection()== LedgerEntry.moneyDirection.DEBIT){
                debitEntry=entry;
            }
        }
        assertEquals(0,BigDecimal.valueOf(1250).compareTo(creditEntry.getAmount()));
        assertEquals(0,BigDecimal.valueOf(800).compareTo(debitEntry.getAmount()));
    }

    @Test
    void InsufficientFunds(){
        Account account= walletService.createAccount("M2","INR");
        String accountId= account.getAccountId();
        CreditDto creditDto=walletService.credit(accountId,BigDecimal.valueOf(300),
                "PAYMENT","PO_2001");

        assertThrows(IllegalArgumentException.class ,()-> walletService.debit(accountId,
                BigDecimal.valueOf(500),"PAYOUT","PO_3001"),
                "Insufficient Funds");

        assertEquals(0,BigDecimal.valueOf(300).compareTo(
                walletService.getBalance(accountId).getCurrentBalance()));

        List<LedgerEntry> entriesM=ledgerEntryRepository.findByAccountId(accountId,0,10);
        List<LedgerEntry> entriesP=ledgerEntryRepository.findByAccountId("PLATFORM",0,10);

        assertEquals(1,entriesM.size());
        assertEquals(LedgerEntry.moneyDirection.CREDIT,
                entriesM.get(0).getDirection());
        assertEquals(0,BigDecimal.valueOf(300).
                compareTo(entriesM.get(0).getAmount()));

        assertEquals(1,entriesP.size());
        assertEquals(LedgerEntry.moneyDirection.DEBIT,
                entriesP.get(0).getDirection());
        assertEquals(0,BigDecimal.valueOf(300).
                compareTo(entriesP.get(0).getAmount()));
    }

    @Test
    void DuplicateActiveAccount(){
        Account account= walletService.createAccount("M3","INR");

        assertThrows(ConflictException.class,
                ()->walletService.createAccount("M3","INR"),
                "Account already exists");

        Optional<Account> account1=accountRepository.findActiveByMerchantIdAndCurrency("M3","INR");
        assertTrue(account1.isPresent());
    }


}
