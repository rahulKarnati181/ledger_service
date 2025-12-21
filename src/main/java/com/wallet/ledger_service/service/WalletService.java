package com.wallet.ledger_service.service;

import com.wallet.ledger_service.Domain.Account;
import com.wallet.ledger_service.Domain.LedgerEntry;
import com.wallet.ledger_service.dto.AccountBalanceDto;
import com.wallet.ledger_service.dto.CreditDto;
import com.wallet.ledger_service.dto.LLedgerEntryDto;
import com.wallet.ledger_service.exception.ConflictException;
import com.wallet.ledger_service.repository.AccountRepository;
import com.wallet.ledger_service.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class WalletService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private static final String PLATFORM_ACCOUNT_ID = "PLATFORM";


    public WalletService(AccountRepository accountRepository,
                         LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public Account createAccount(String merchantId,String currency){
        if(merchantId==null||currency==null||
                merchantId.isBlank()||currency.isBlank()){
            throw new IllegalArgumentException("MerchantId or currency not present");
        }
        Optional <Account> accountcheck =accountRepository.findActiveByMerchantIdAndCurrency(merchantId, currency);
        if(accountcheck.isPresent()){
            throw new ConflictException("Account already exists");
        }
        Account account=new Account();
        account.setAccountId(UUID.randomUUID().toString());
        account.setMerchantId(merchantId);
        account.setCurrency(currency);
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        return(accountRepository.save(account));
    }
    public Account getAccount(String accountId){
        if(accountId==null||accountId.isBlank()){
            throw new IllegalArgumentException("accountId empty");
        }
        Optional<Account> account=accountRepository.findById(accountId);
        if(account.isEmpty()){
            throw new IllegalArgumentException("not found");
        }else{
            return(account.get());
        }
    }
    public AccountBalanceDto getBalance(String accountId) {
        if(accountId==null||accountId.isBlank()){
            throw new IllegalArgumentException("Wrong Input");
        }
        Optional<Account> account = accountRepository.findById(accountId);
        if (account.isEmpty()) {
            throw new IllegalArgumentException("not found");
        } else {
            AccountBalanceDto accountBalanceDto = new AccountBalanceDto();
            accountBalanceDto.setAccountID(account.get().getAccountId());
            accountBalanceDto.setCurrentBalance(account.get().getCurrentBalance());
            accountBalanceDto.setCurrency(account.get().getCurrency());
            accountBalanceDto.setStatus(account.get().getStatus());
            return(accountBalanceDto);
        }
    }
    public CreditDto credit(String accountId, BigDecimal amount,
                            String referenceType, String referenceId){
        if(amount.compareTo(BigDecimal.ZERO )<=0 || referenceId==null||
        referenceId.isBlank()||referenceType==null||referenceType.isBlank()
                || accountId==null||accountId.isBlank()){
            throw new IllegalArgumentException("Wrong Input");
        }
        Optional<Account> account=accountRepository.findById(accountId);

        if(account.isEmpty()){
            throw new IllegalArgumentException("Account not Present");
        }
        if(!(account.get().getStatus()==
                Account.AccountStatus.ACTIVE )){
            throw new ConflictException("Account not Active");
        }
        LedgerEntry entry1=new LedgerEntry();
        LedgerEntry entry2=new LedgerEntry();

        entry1.setId(UUID.randomUUID().toString());
        entry1.setAccountId(accountId);
        entry1.setCounterpartyAccountId(PLATFORM_ACCOUNT_ID);
        entry1.setDirection(LedgerEntry.moneyDirection.CREDIT);
        entry1.setType(LedgerEntry.moneyType.PAYMENT);
        entry1.setAmount(amount);
        entry1.setReferenceType(referenceType);
        entry1.setReferenceId(referenceId);
        entry1.setCreatedAt(Instant.now());

        entry2.setId(UUID.randomUUID().toString());
        entry2.setAccountId(PLATFORM_ACCOUNT_ID);
        entry2.setCounterpartyAccountId(accountId);
        entry2.setDirection(LedgerEntry.moneyDirection.DEBIT);
        entry2.setType(LedgerEntry.moneyType.PAYMENT);
        entry2.setAmount(amount);
        entry2.setReferenceType(referenceType);
        entry2.setReferenceId(referenceId);
        entry2.setCreatedAt(Instant.now());

        ledgerEntryRepository.saveAll(List.of(entry1,entry2));
        account.get().setCurrentBalance(account.get().getCurrentBalance().add(amount));
        Account account1=accountRepository.save(account.get());

        CreditDto creditDto=new CreditDto();
        creditDto.setAccountId(accountId);
        creditDto.setNewBalance(account1.getCurrentBalance());
        creditDto.setAmount(amount);
        creditDto.setReferenceType(referenceType);
        creditDto.setReferenceId(referenceId);
        creditDto.setOperationId(entry1.getId());
        creditDto.setTimestamp(Instant.now());

        return creditDto;
    }

    public CreditDto debit(String accountId,BigDecimal amount , String referenceType,
                           String referenceId){
        if(amount.compareTo(BigDecimal.ZERO )<=0 || referenceId==null||
                referenceId.isBlank()||referenceType==null||referenceType.isBlank()
                || accountId==null||accountId.isBlank()){
            throw new IllegalArgumentException("Wrong Input");
        }

        if(accountRepository.findById(accountId).isEmpty()){
            throw new IllegalArgumentException("Account not Present");
        }

        Account account=accountRepository.findById(accountId).get();
        if(!(account.getStatus()==
                Account.AccountStatus.ACTIVE )){
            throw new ConflictException("Account not Active");
        }
        if(account.getCurrentBalance().compareTo(amount)<0 ){
            throw new IllegalArgumentException("Insufficient funds");
        }
        LedgerEntry entry1=new LedgerEntry();
        LedgerEntry entry2=new LedgerEntry();

        entry1.setId(UUID.randomUUID().toString());
        entry1.setAccountId(accountId);
        entry1.setCounterpartyAccountId(PLATFORM_ACCOUNT_ID);
        entry1.setDirection(LedgerEntry.moneyDirection.DEBIT);
        entry1.setType(LedgerEntry.moneyType.PAYMENT);
        entry1.setAmount(amount);
        entry1.setReferenceType(referenceType);
        entry1.setReferenceId(referenceId);
        entry1.setCreatedAt(Instant.now());

        entry2.setId(UUID.randomUUID().toString());
        entry2.setAccountId(PLATFORM_ACCOUNT_ID);
        entry2.setCounterpartyAccountId(accountId);
        entry2.setDirection(LedgerEntry.moneyDirection.CREDIT);
        entry2.setType(LedgerEntry.moneyType.PAYMENT);
        entry2.setAmount(amount);
        entry2.setReferenceType(referenceType);
        entry2.setReferenceId(referenceId);
        entry2.setCreatedAt(Instant.now());

        ledgerEntryRepository.saveAll(List.of(entry1,entry2));
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));

        Account account2=accountRepository.save(account);

        CreditDto creditDto=new CreditDto();
        creditDto.setAccountId(accountId);
        creditDto.setNewBalance(account.getCurrentBalance());
        creditDto.setAmount(amount);
        creditDto.setReferenceType(referenceType);
        creditDto.setReferenceId(referenceId);
        creditDto.setOperationId(entry1.getId());
        creditDto.setTimestamp(Instant.now());

        return creditDto;
    }

    public List<LLedgerEntryDto> listLedgerEntries(String accountId,int page,int size){
        Optional<Account> account1=accountRepository.findById(accountId);

        if(account1.isEmpty()){
            throw new IllegalArgumentException("Account not Present");
        }
        Account account=account1.get();
        List<LedgerEntry> entries=  ledgerEntryRepository.findByAccountId(accountId,page,size);
        List<LLedgerEntryDto> entryDtos= new ArrayList<>();
        boolean check;
        for(int i=0;i<entries.size();i++){
            LLedgerEntryDto entryDto=new LLedgerEntryDto();
            entryDto.setAccountId(accountId);
            entryDto.setId(entries.get(i).getId());
            entryDto.setAmount(entries.get(i).getAmount());
            entryDto.setDirection(entries.get(i).getDirection());
            entryDto.setType(entries.get(i).getType());
            entryDto.setReferenceType(entries.get(i).getReferenceType());
            entryDto.setReferenceId(entries.get(i).getReferenceId());
            entryDto.setCounterpartyAccountId(entries.get(i)
                    .getCounterpartyAccountId());
            entryDto.setCreatedAt(entries.get(i).getCreatedAt());
            entryDto.setPage(page);
            entryDto.setSize(size);
            check= entries.size() == size;
            entryDto.setHasNext(check);
            entryDtos.add(entryDto);
        }
        return(entryDtos);
    }


}
