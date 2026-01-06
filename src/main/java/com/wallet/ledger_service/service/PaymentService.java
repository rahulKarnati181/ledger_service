package com.wallet.ledger_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.ledger_service.Domain.Account;
import com.wallet.ledger_service.Domain.LedgerEntry;
import com.wallet.ledger_service.Domain.PaymentEvent;
import com.wallet.ledger_service.Domain.PaymentIntent;
import com.wallet.ledger_service.dto.CheckoutResponseDto;
import com.wallet.ledger_service.dto.LLedgerEntryDto;
import com.wallet.ledger_service.exception.ConflictException;
import com.wallet.ledger_service.provider.PaymentProvider;
import com.wallet.ledger_service.provider.ProviderPaymentCreationResult;
import com.wallet.ledger_service.provider.ProviderWebhookResult;
import com.wallet.ledger_service.repository.JpaPaymentEventRepository;
import com.wallet.ledger_service.repository.JpaPaymentIntentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final JpaPaymentEventRepository jpaPaymentEventRepository;
    private final JpaPaymentIntentRepository jpaPaymentIntentRepository;
    private final WalletService walletService;
    private final PaymentProvider paymentProvider;
    private final ObjectMapper objectMapper;

    public PaymentService(JpaPaymentIntentRepository jpaPaymentIntentRepository,
                          JpaPaymentEventRepository jpaPaymentEventRepository,
                          WalletService walletService, PaymentProvider paymentProvider,
                          ObjectMapper objectMapper
                          ){
        this.jpaPaymentEventRepository=jpaPaymentEventRepository;
        this.jpaPaymentIntentRepository=jpaPaymentIntentRepository;
        this.walletService=walletService;
        this.paymentProvider = paymentProvider;
        this.objectMapper=objectMapper;
    }


    public CheckoutResponseDto checkoutflow(String accountId, BigDecimal amount,
                                            String currency, PaymentIntent.Provider provider,
                                            String idempotencyKey){
        //Input Validation
        if(accountId == null || accountId.isBlank() || amount.compareTo(BigDecimal.ZERO) <= 0 ||
                currency == null || currency.isBlank() || provider == null
                 || idempotencyKey == null || idempotencyKey.isBlank()){
            throw new IllegalArgumentException("Invalid input");
        }
        //Account Status
        if(!(walletService.getAccount(accountId).getStatus()== Account.AccountStatus.ACTIVE)){
            throw new IllegalArgumentException("Account not active");
        }

        Optional<PaymentIntent> exists =jpaPaymentIntentRepository.
                findByAccountIdAndIdempotencyKey(accountId,idempotencyKey);

        PaymentIntent paymentIntent;
        if(exists.isPresent()){
              paymentIntent = exists.get();
            if (paymentIntent.getAmount().compareTo(amount) != 0 ||
                    !paymentIntent.getCurrency().equals(currency) ||
                    paymentIntent.getProvider() != provider) {
                throw new ConflictException("Idempotency key already used with different payment details");
            }
             if(paymentIntent.getProviderPaymentId()!=null){

             CheckoutResponseDto checkoutResponseDto=new CheckoutResponseDto();
             checkoutResponseDto.setProviderPaymentId(paymentIntent.getProviderPaymentId());
             checkoutResponseDto.setPaymentIntentId(paymentIntent.getId());
             checkoutResponseDto.setStatus(paymentIntent.getStatus());
             checkoutResponseDto.setIdempotencyHit(true);
             return(checkoutResponseDto);}

        }else {
            paymentIntent=new PaymentIntent();
            paymentIntent.setId(String.valueOf(UUID.randomUUID()));
            paymentIntent.setAccountId(accountId);
            paymentIntent.setAmount(amount);
            paymentIntent.setCurrency(currency);
            paymentIntent.setProvider(provider);
            paymentIntent.setIdempotencyKey(idempotencyKey);
            try{
                jpaPaymentIntentRepository.save(paymentIntent);}
            catch (DataIntegrityViolationException e){
                // Handle rare race condition if another thread already created intent with same idempotency key

                Optional<PaymentIntent> paymentRace =jpaPaymentIntentRepository.
                        findByAccountIdAndIdempotencyKey(accountId,idempotencyKey);
                if(paymentRace.isEmpty()){
                    throw e;
                }
                paymentIntent = paymentRace.get();
                if (paymentIntent.getAmount().compareTo(amount) != 0 ||
                        !paymentIntent.getCurrency().equals(currency) ||
                        paymentIntent.getProvider() != provider) {
                    throw new ConflictException(
                            "Idempotency key already used with different payment details"
                    );
                }
            }
        }
        if (paymentIntent.getProviderPaymentId() != null) {
            CheckoutResponseDto checkoutResponseDto=new CheckoutResponseDto();
            checkoutResponseDto.setProviderPaymentId(paymentIntent.getProviderPaymentId());
            checkoutResponseDto.setPaymentIntentId(paymentIntent.getId());
            checkoutResponseDto.setStatus(paymentIntent.getStatus());
            checkoutResponseDto.setIdempotencyHit(true);
            return(checkoutResponseDto); // double safety
        }


        ProviderPaymentCreationResult providerPaymentCreationResult=paymentProvider.
                createPayment(paymentIntent.getId(),
                        paymentIntent.getAmount(),
                        paymentIntent.getCurrency());
        paymentIntent.setProviderPaymentId(providerPaymentCreationResult.providerPaymentId());
        paymentIntent.transitionTo(PaymentIntent.PaymentStatus.PROCESSING);
        jpaPaymentIntentRepository.save(paymentIntent);

        CheckoutResponseDto checkoutResponseDto=new CheckoutResponseDto();
        checkoutResponseDto.setPaymentIntentId(paymentIntent.getId());
        checkoutResponseDto.setStatus(paymentIntent.getStatus());
        checkoutResponseDto.setProviderPaymentId(paymentIntent.getProviderPaymentId());
        checkoutResponseDto.setIdempotencyHit(false);
        return (checkoutResponseDto);
    }

    @Transactional
    public void handleWebhook (PaymentIntent.Provider provider,String payload){

        ProviderWebhookResult providerWebhookResult=paymentProvider.interpretWebhook(payload);
        PaymentEvent paymentEvent=new PaymentEvent();
        paymentEvent.setId(String.valueOf(UUID.randomUUID()));
        paymentEvent.setProvider(provider);
        paymentEvent.setProviderEventId(providerWebhookResult.providerEventId());
        paymentEvent.setProviderPaymentId(providerWebhookResult.providerPaymentId());
        paymentEvent.setEventType(String.valueOf(providerWebhookResult.status()));

        Map<String, Object> payloadMap;
        try {
            payloadMap = objectMapper.readValue(
                    payload,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid webhook payload", e);
        }
        paymentEvent.setPayload(payloadMap);

        try{
            jpaPaymentEventRepository.save(paymentEvent);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e, "payment_events_prov_pei")) {
                return;
            }

            // Anything else is a real bug
            throw e;

        }catch (Exception e){
            throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
        }

        Optional<PaymentIntent> paymentIntent1 =jpaPaymentIntentRepository.
                findByProviderAndProviderPaymentId(provider,
                providerWebhookResult.providerPaymentId());

        if(paymentIntent1.isEmpty()){
            return;
        }
        PaymentIntent paymentIntent=paymentIntent1.get();

        if(paymentIntent.getStatus().equals(PaymentIntent.PaymentStatus.SUCCEEDED)||
        paymentIntent.getStatus().equals(PaymentIntent.PaymentStatus.FAILED)||
        paymentIntent.getStatus().equals(PaymentIntent.PaymentStatus.CANCELLED)){
            return;
        }
        if(paymentIntent.getStatus().equals(PaymentIntent.PaymentStatus.CREATED)){
            paymentIntent.transitionTo(PaymentIntent.PaymentStatus.PROCESSING);
        }
        if (providerWebhookResult.status().equals(ProviderWebhookResult.ResultStatus.CANCELLED)) {
            paymentIntent.transitionTo(PaymentIntent.PaymentStatus.CANCELLED);
            jpaPaymentIntentRepository.save(paymentIntent);
            return;
        }


        if(providerWebhookResult.status().equals(ProviderWebhookResult.ResultStatus.FAILED)){
            paymentIntent.transitionTo(PaymentIntent.PaymentStatus.FAILED);
            jpaPaymentIntentRepository.save(paymentIntent);
        }
        if(providerWebhookResult.status().equals(ProviderWebhookResult.ResultStatus.SUCCEEDED)){
            if(paymentIntent.getWalletOperationId() != null &&
                    ! paymentIntent.getWalletOperationId().isBlank()){
                if(!paymentIntent.getStatus().equals(PaymentIntent.PaymentStatus.SUCCEEDED)){
                    paymentIntent.transitionTo(PaymentIntent.PaymentStatus.SUCCEEDED);
                    jpaPaymentIntentRepository.save(paymentIntent);
                }
                return;
            }else{
            try{
            paymentIntent.setWalletOperationId(
            walletService.credit(paymentIntent.getAccountId(),paymentIntent.getAmount(),
                    "PAYMENT_INTENT",paymentIntent.getId()).getOperationId());
            }
            catch (DataIntegrityViolationException e){
                if (isUniqueViolation(e, "uq_credit_once")) {
                    Optional<LedgerEntry> ledgerEntry =
                            walletService.findCreditForPaymentIntent(
                                    paymentIntent.getAccountId(),
                                    paymentIntent.getId()
                            );

                    if (ledgerEntry.isEmpty()) {
                        throw new IllegalStateException(
                                "Thread Errors",
                                e
                        );
                    }
                    paymentIntent.setWalletOperationId(ledgerEntry.get().getId());
                }else{
                    throw e;
                }
            }catch (Exception e){
                throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
            }

            paymentIntent.transitionTo(PaymentIntent.PaymentStatus.SUCCEEDED);
            jpaPaymentIntentRepository.save(paymentIntent);}
        }
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e, String constraintName) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve) {
                if ("23505".equals(cve.getSQLState())) {
                    return constraintName == null ||
                            constraintName.equalsIgnoreCase(cve.getConstraintName());
                }
            }
            cause = cause.getCause();
        }
        return false;
    }



}
