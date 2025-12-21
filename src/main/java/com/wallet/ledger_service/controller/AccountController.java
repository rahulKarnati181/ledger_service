package com.wallet.ledger_service.controller;

import com.wallet.ledger_service.Domain.Account;
import com.wallet.ledger_service.dto.*;
import com.wallet.ledger_service.exception.ConflictException;
import com.wallet.ledger_service.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final WalletService walletService;

    public AccountController(WalletService walletService){
        this.walletService=walletService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse HandleExp(IllegalArgumentException e){
        ErrorResponse er=new ErrorResponse();
        er.setErrorCode("BAD_REQUEST");
        er.setMessage(e.getMessage());
        return(er);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse HandleCon(ConflictException e){
        ErrorResponse er = new ErrorResponse();
        er.setErrorCode("CONFLICT");
        er.setMessage(e.getMessage());
        return er;
    }


    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody
            CreateAccountRequestDto createAccountRequestDto){
        return walletService.createAccount(
                createAccountRequestDto.getMerchantId()
                ,createAccountRequestDto.getCurrency());
    }

    @GetMapping("/{accountId}")
    public Account getAccountDetails(@PathVariable
                                     String accountId){
        return (walletService.getAccount(accountId));
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceDto getAccountBalance(@PathVariable
                                               String accountId){
        return (walletService.getBalance(accountId));
    }

    @PostMapping("/{accountId}/credit")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditDto CreditMoney(@PathVariable String accountId,
                                 @RequestBody
                                 CreateCreditRequestDto createCreditRequestDto){
        return (walletService.credit( accountId,
                createCreditRequestDto.getAmount(),
                createCreditRequestDto.getReferenceType(),
                createCreditRequestDto.getReferenceId()));
    }

    @PostMapping("/{accountId}/debit")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditDto DebitMoney(@PathVariable String accountId,
                                @RequestBody
                                CreateCreditRequestDto createCreditRequestDto){
        return (walletService.debit( accountId,
                createCreditRequestDto.getAmount(),
                createCreditRequestDto.getReferenceType(),
                createCreditRequestDto.getReferenceId()));
    }

    @GetMapping("/{accountId}/ledger")
    public List<LLedgerEntryDto> listLedger(@PathVariable String accountId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size){

        return(walletService.listLedgerEntries(accountId, page, size));
    }




}
