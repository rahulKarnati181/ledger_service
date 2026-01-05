package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.PaymentIntent;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaPaymentIntentRepository implements PaymentIntentRepository{

    private final PaymentIntentJpaRepository paymentIntentJpaRepository;
    JpaPaymentIntentRepository (PaymentIntentJpaRepository paymentIntentJpaRepository){
        this.paymentIntentJpaRepository=paymentIntentJpaRepository;
    }
    @Override
    public PaymentIntent save(PaymentIntent paymentIntent){
        return(paymentIntentJpaRepository.save(paymentIntent) );
    }
    @Override
    public Optional<PaymentIntent> findById(String paymentIntentId){
        return (paymentIntentJpaRepository.findById(paymentIntentId));
    }
    @Override
    public Optional<PaymentIntent> findByAccountIdAndIdempotencyKey(String accountId,String idempotencyKey){
        return(paymentIntentJpaRepository.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey));
    }
    @Override
    public Optional<PaymentIntent> findByProviderAndProviderPaymentId(PaymentIntent.Provider provider,String providerPaymentId){
        return(paymentIntentJpaRepository.findByProviderAndProviderPaymentId(provider,providerPaymentId));
    }


}
